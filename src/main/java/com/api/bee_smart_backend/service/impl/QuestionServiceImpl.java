package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuestionRequest;
import com.api.bee_smart_backend.helper.response.QuestionResponse;
import com.api.bee_smart_backend.model.Question;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.repository.QuestionRepository;
import com.api.bee_smart_backend.repository.QuizRepository;
import com.api.bee_smart_backend.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuestionRepository questionRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public QuestionResponse addQuestionToQuiz(String quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Quiz not found", HttpStatus.NOT_FOUND));

        Question question = Question.builder()
                .content(request.getContent())
                .image(request.getImage())
                .options(request.getOptions())
                .correctAnswerIndex(request.getCorrectAnswerIndex())
                .quiz(quiz)
                .createdAt(now)
                .build();

        Question savedQuestion = questionRepository.save(question);
        quiz.addQuestion(savedQuestion);
        quizRepository.save(quiz);

        return mapData.mapOne(savedQuestion, QuestionResponse.class);
    }
}
