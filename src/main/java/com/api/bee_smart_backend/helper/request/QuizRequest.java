package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class QuizRequest {
    private String title;
    private String description;
    private String image;
    private int quizDuration;
}
