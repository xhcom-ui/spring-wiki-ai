package org.wiki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档元信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDoc {

    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private LocalDateTime uploadTime;
    private String status;
}
