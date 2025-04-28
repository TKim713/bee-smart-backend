package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.QuestionRequest;
import com.api.bee_smart_backend.helper.response.QuestionResponse;
import com.api.bee_smart_backend.model.Question;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QuestionService {
    QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request);

    QuestionResponse updateQuestionByQuestionId(String questionId, QuestionRequest request);

    void deleteQuestionsByQuestionIds(List<String> questionIds);

    QuestionResponse getQuestionByQuestionId(String questionId);

    Map<String, Object> getListQuestionsByQuizId(String quizId, String page, String size, String search);

    Map<String, Object> getQuestionsByQuizId(String quizId);

    boolean checkAnswer(String questionId, String userAnswer);

    Question getRandomQuestionByGradeAndSubject(String gradeId, String subjectId, Set<String> excludeIds);
}
