package org.wiki.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorFilms {
    private String actor;
    private List<String> movies;
    private String careerPeriod;
    private int totalFilms;
    private String famousRole;
}