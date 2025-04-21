package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.request.UserAnswer;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class QuizControllerTest {
    @InjectMocks
    private QuizController quizController;

    @Mock
    private QuizService quizService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizRecordRepository quizRecordRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private UserAnswer mockUserAnswer(String questionId, String selectedAnswer, List<String> selectedAnswers) {
        return UserAnswer.builder()
                .questionId(questionId)
                .selectedAnswer(selectedAnswer)
                .selectedAnswers(selectedAnswers)
                .build();
    }

    @Test
    void testSubmitQuiz_Success() {
        String token = "Bearer mockToken";
        String quizId = "quiz123";
        SubmissionRequest request = new SubmissionRequest();
        request.setAnswers(List.of(
                mockUserAnswer("q1", "A", new ArrayList<>()),
                mockUserAnswer("q2", "B", new ArrayList<>())
        ));
        request.setTimeSpent(300);

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("totalQuestions", 2);
        mockResponse.put("correctAnswers", 2);
        mockResponse.put("points", 10.0);
        mockResponse.put("quizDuration", 600);

        when(quizService.submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString()))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<ResponseObject<Map<String, Object>>> response = quizController.submitQuiz(
                token, quizId, request, "0", "10");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) response.getBody().getData();

        assertEquals(10.0, responseData.get("points"));
        assertEquals(2, responseData.get("totalQuestions"));

        verify(quizService, times(1)).submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString());
    }

    @Test
    void testSubmitQuiz_QuizNotFound() {
        // Arrange
        String token = "Bearer mockToken";
        String quizId = "quiz123";
        SubmissionRequest request = new SubmissionRequest();
        when(quizService.submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString()))
                .thenThrow(new CustomException("Không tìm thấy quiz", HttpStatus.NOT_FOUND));

        // Act
        ResponseEntity<ResponseObject<Map<String, Object>>> response = quizController.submitQuiz(
                token, quizId, request, "0", "10");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Không tìm thấy quiz", response.getBody().getMessage());

        verify(quizService, times(1)).submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString());
    }

    @Test
    void testSubmitQuiz_InvalidQuestionType() {
        // Arrange
        String token = "Bearer mockToken";
        String quizId = "quiz123";
        SubmissionRequest request = new SubmissionRequest();
        when(quizService.submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString()))
                .thenThrow(new CustomException("Loại câu hỏi không hợp lệ", HttpStatus.BAD_REQUEST));

        // Act
        ResponseEntity<ResponseObject<Map<String, Object>>> response = quizController.submitQuiz(
                token, quizId, request, "0", "10");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Loại câu hỏi không hợp lệ", response.getBody().getMessage());

        verify(quizService, times(1)).submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString());
    }

    @Test
    void testSubmitQuiz_InternalServerError() {
        // Arrange
        String token = "Bearer mockToken";
        String quizId = "quiz123";
        SubmissionRequest request = new SubmissionRequest();
        when(quizService.submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<ResponseObject<Map<String, Object>>> response = quizController.submitQuiz(
                token, quizId, request, "0", "10");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Lỗi nộp bài"));

        verify(quizService, times(1)).submitQuiz(anyString(), eq(quizId), any(SubmissionRequest.class), anyString(), anyString());
    }
}
