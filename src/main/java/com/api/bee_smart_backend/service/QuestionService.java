package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.QuestionRequest;
import com.api.bee_smart_backend.helper.response.QuestionResponse;

import java.util.List;
import java.util.Map;

public interface QuestionService {
    QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request);

    QuestionResponse updateQuestionByQuestionId(String questionId, QuestionRequest request);

    void deleteQuestionsByQuestionIds(List<String> questionIds);

    Map<String, Object> getListQuestionsByQuizId(String quizId, String page, String size, String search);
}
