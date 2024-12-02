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
public class SubmissionResponse {
    private int totalQuestions;
    private int correctAnswers;
    private double points;
    private List<QuestionResult> results;
}
