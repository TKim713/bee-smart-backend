package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class QuizResponse {
    private String quizId;
    private String title;
    private String description;
    private String image;
    private int quizDuration;
    private String lessonId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
