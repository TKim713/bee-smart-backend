package com.api.bee_smart_backend.model.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerScore {
    private String userId;
    private String username;
    private int score;
    private int correctAnswers; // New field for correct answers
    private int incorrectAnswers;
}
