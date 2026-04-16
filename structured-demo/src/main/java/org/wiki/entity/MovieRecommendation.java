package org.wiki.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRecommendation {
    private String title;
    private int releaseYear;
    private String director;
    private List<String> genres;
    private String language;
    private double rating;
    private int duration;
    private String summary;
    private List<String> mainActors;
}