package org.wiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;      // 统计内容
    private int tokenCount;      // Token 数量
    private String type;         // single / conversation
    private LocalDateTime createTime;
}