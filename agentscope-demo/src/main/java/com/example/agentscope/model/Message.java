package com.example.agentscope.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long conversationId;
    private String role; // user, assistant
    private String content;
    private String type; // text, tool_call, tool_response
    private LocalDateTime createdAt;
}
