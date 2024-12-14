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
public class TopicLessonResponse {
    private String topicId;
    private String topicName;
    private int topicNumber;
    private List<LessonResponse> lessons;
    private List<QuizResponse> quizzes;
}
