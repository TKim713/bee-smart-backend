package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class QuizRecordResponse {
    private String recordId;
    private String username;
    private String quizName;
    private int totalQuestions;
    private int correctAnswers;
    private double points;
    private long timeSpent;
    private List<QuestionResult> questionResults;
    private Instant createdAt;
}
