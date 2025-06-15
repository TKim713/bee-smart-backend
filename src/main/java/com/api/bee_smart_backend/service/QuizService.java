package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.QuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.SubmissionResponse;
import com.api.bee_smart_backend.model.record.QuizRecord;

import java.util.List;
import java.util.Map;

public interface QuizService {
    QuizResponse getQuizById(String quizId);

    QuizResponse createQuizByLessonId(String topicId, QuizRequest request);

    QuizResponse createQuizByTopicId(String topicId, QuizRequest request);

    QuizResponse updateQuiz(String quizId, QuizRequest request);

    List<String> deleteQuizzes(List<String> quizIds);

    Map<String, Object> getQuizzesByTopic(String topicId, String page, String size, String search);

    Map<String, Object> getQuizzesByLessonId(String lessonId, String page, String size);

    Map<String, Object> submitQuiz(String jwtToken, String quizId, SubmissionRequest request, String page, String size);

    Map<String, Object> getQuizRecordsByUser(String userId, String page, String size);

    QuizRecordResponse getQuizRecordById(String jwtToken, String recordId);
}
