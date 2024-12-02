package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.QuestionRequest;
import com.api.bee_smart_backend.helper.response.QuestionResponse;

public interface QuestionService {
    QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request);
}
