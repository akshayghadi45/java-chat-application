package com.chatapp.controller;
import com.chatapp.model.Message;
import com.chatapp.model.dto.ClientMessage;
import com.chatapp.security.JwtTokenProvider;
import com.chatapp.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest; // ✅ Correct for Spring Boot 3+
import jakarta.validation.Valid; // ✅ Correct for Spring Boot 3+

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    private final JwtTokenProvider jwtTokenProvider;
    public MessageController(MessageService messageService, JwtTokenProvider jwtTokenProvider) {
        this.messageService = messageService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    @GetMapping
    public ResponseEntity<List<ClientMessage>> getMessages(
            @RequestParam Long userId,
            HttpServletRequest request) {


        String token = jwtTokenProvider.resolveToken(request);
        Long currentUserId = jwtTokenProvider.getUserId(token);

        return ResponseEntity.ok(messageService.getFormattedConversation(currentUserId, userId));
    }
    @PostMapping
    public ResponseEntity<Message> createMessage(
            @Valid @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        String token = jwtTokenProvider.resolveToken(request);
        Long senderId = jwtTokenProvider.getUserId(token);
        Long receiverId = Long.parseLong(payload.get("receiverId").toString());
        String text = payload.get("text").toString();

        Message message = messageService.createMessage(senderId, receiverId, text);
        return ResponseEntity.ok(message);
    }
}