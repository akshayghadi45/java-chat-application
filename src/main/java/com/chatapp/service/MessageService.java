package com.chatapp.service;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.model.dto.ClientMessage;
import com.chatapp.repository.MessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserService userService;
    public MessageService(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }
    public Message createMessage(Long senderId, Long receiverId, String text) {
        Optional<User> senderOpt = userService.getUserById(senderId);
        Optional<User> receiverOpt = userService.getUserById(receiverId);

        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender or receiver does not exist");
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setText(text);

        return messageRepository.save(message);
    }
    public List<Message> getConversation(Long user1Id, Long user2Id) {
        return messageRepository.findConversation(user1Id, user2Id);
    }
    public List<ClientMessage> getFormattedConversation(Long currentUserId, Long otherUserId) {
        List<Message> messages = getConversation(currentUserId, otherUserId);

        return messages.stream().map(message -> {
            User sender = userService.getUserById(message.getSenderId()).orElseThrow();
            boolean isCurrentUser = message.getSenderId().equals(currentUserId);

            return ClientMessage.builder()
                    .id(message.getId())
                    .sender(sender.getUsername())
                    .text(message.getText())
                    .timestamp(message.getTimestamp())
                    .isCurrentUser(isCurrentUser)
                    .build();
        }).collect(Collectors.toList());
    }
}