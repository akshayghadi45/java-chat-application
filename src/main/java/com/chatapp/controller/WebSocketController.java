package com.chatapp.controller;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.model.dto.ClientMessage;
import com.chatapp.model.dto.UserDTO;
import com.chatapp.model.dto.WebSocketMessage;
import com.chatapp.security.JwtTokenProvider;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Controller
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final MessageService messageService;

    private final Map<String, Long> sessionUserMap = new HashMap<>();
    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               JwtTokenProvider jwtTokenProvider,
                               UserService userService,
                               MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.messageService = messageService;
    }
    @MessageMapping("/chat")
    public void processMessage(@Payload WebSocketMessage socketMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        try {
            String token = socketMessage.getToken();
            String type = socketMessage.getType();

            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);
                Optional<User> user = userService.getUserById(userId);

                if (user.isPresent()) {
                    switch (type) {
                        case "connection":
                            handleConnection(sessionId, userId);
                            break;

                        case "message":
                            handleMessage(socketMessage, userId);
                            break;

                        case "requestHistory":
                            handleHistoryRequest(socketMessage, userId);
                            break;

                        case "disconnection":
                            handleDisconnection(sessionId);
                            break;
                    }
                } else {
                    sendError(sessionId, "User not found");
                }
            } else {
                sendError(sessionId, "Invalid authentication");
            }
        } catch (Exception e) {
            sendError(sessionId, "Error processing message: " + e.getMessage());
        }
    }

    private void handleConnection(String sessionId, Long userId) {
        sessionUserMap.put(sessionId, userId);
        userService.updateUserOnlineStatus(userId, true);

        // Notify all users about updated user list
        broadcastUserList();
    }

    private void handleDisconnection(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            userService.updateUserOnlineStatus(userId, false);
            broadcastUserList();
        }
    }

    private void handleMessage(WebSocketMessage socketMessage, Long senderId) {
        Map<String, Object> payload = (Map<String, Object>) socketMessage.getPayload();
        String text = (String) payload.get("text");
        Long receiverId = Long.parseLong(payload.get("targetUserId").toString());

        Message message = messageService.createMessage(senderId, receiverId, text);
        User sender = userService.getUserById(senderId).orElseThrow();

        // Format message for sender
        ClientMessage clientMessageForSender = ClientMessage.builder()
                .id(message.getId())
                .sender(sender.getUsername())
                .text(message.getText())
                .timestamp(message.getTimestamp())
                .isCurrentUser(true)
                .build();

        // Send message to sender
        WebSocketMessage senderMessage = new WebSocketMessage(
                "message",
                Map.of("message", clientMessageForSender),
                null
        );
        messagingTemplate.convertAndSendToUser(
                sender.getUsername(),
                "/queue/messages",
                senderMessage
        );

        // Format message for receiver and send if they are connected
        Optional<User> receiverOpt = userService.getUserById(receiverId);
        if (receiverOpt.isPresent()) {
            User receiver = receiverOpt.get();
            ClientMessage clientMessageForReceiver = ClientMessage.builder()
                    .id(message.getId())
                    .sender(sender.getUsername())
                    .text(message.getText())
                    .timestamp(message.getTimestamp())
                    .isCurrentUser(false)
                    .build();

            WebSocketMessage receiverMessage = new WebSocketMessage(
                    "message",
                    Map.of("message", clientMessageForReceiver),
                    null
            );

            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/messages",
                    receiverMessage
            );
        }
    }

    private void handleHistoryRequest(WebSocketMessage socketMessage, Long userId) {
        Map<String, Object> payload = (Map<String, Object>) socketMessage.getPayload();
        Long targetUserId = Long.parseLong(payload.get("targetUserId").toString());

        User user = userService.getUserById(userId).orElseThrow();
        List<ClientMessage> messages = messageService.getFormattedConversation(userId, targetUserId);

        WebSocketMessage historyMessage = new WebSocketMessage(
                "history",
                Map.of("messages", messages),
                null
        );

        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/messages",
                historyMessage
        );
    }

    private void broadcastUserList() {
        List<UserDTO> users = userService.getAllUsers();

        WebSocketMessage usersMessage = new WebSocketMessage(
                "users",
                Map.of("users", users),
                null
        );

        messagingTemplate.convertAndSend("/topic/users", usersMessage);
    }

    private void sendError(String sessionId, String errorMessage) {
        WebSocketMessage errorResponse = new WebSocketMessage(
                "error",
                Map.of("message", errorMessage),
                null
        );

        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/errors",
                errorResponse
        );
    }
}