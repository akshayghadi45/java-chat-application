package com.chatapp.model.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMessage {
    private Long id;
    private String sender;
    private String text;
    private Date timestamp;
    private boolean isCurrentUser;
}