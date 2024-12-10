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
public class QuestionResponse {
    private String questionId;
    private String content;
    private String image;
    private List<String> options;
    private String questionType;
    private String correctAnswer;
    private List<String> correctAnswers;
    private List<String> answers;
}
