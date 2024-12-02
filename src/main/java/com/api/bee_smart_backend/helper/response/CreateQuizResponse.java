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
public class CreateQuizResponse {
    private String quizId;
    private String title;
    private String description;
    private String image;
    private String topicId;
    private List<QuestionResponse> questions;
}
