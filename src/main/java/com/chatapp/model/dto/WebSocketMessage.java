package com.chatapp.model.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // "connection", "message", "history", "users", "error"
    private Object payload;
    private String token;
}