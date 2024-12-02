package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.CreateQuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.response.CreateQuizResponse;
import com.api.bee_smart_backend.helper.response.SubmissionResponse;

public interface QuizService {
    CreateQuizResponse createQuiz(String topicId, CreateQuizRequest request);

    SubmissionResponse submitQuiz(String quizId, SubmissionRequest request);
}
