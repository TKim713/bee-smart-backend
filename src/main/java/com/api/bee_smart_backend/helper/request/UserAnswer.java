package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserAnswer {
    private String questionId;
    private int selectedAnswerIndex;
}