package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AnswerRequest {
    private String userId;
    private String questionId;
    private String answer;          // For MULTIPLE_CHOICE and FILL_IN_THE_BLANK
    private List<String> answers;   // For MULTI_SELECT
    private int timeTaken;
}
