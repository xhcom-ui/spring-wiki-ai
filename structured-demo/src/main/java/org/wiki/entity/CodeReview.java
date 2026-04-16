package org.wiki.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeReview {
    private String summary;
    private int score;
    private List<Issue> issues;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        private String file;
        private int line;
        private String severity; // HIGH, MEDIUM, LOW
        private String description;
        private String suggestion;
    }
}