package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class QuestionResult {
    private String questionId;
    private String content;
    private String image;
    private List<String> options;
    private int correctAnswerIndex;
    private int userAnswerIndex;
    private boolean isCorrect;
}
