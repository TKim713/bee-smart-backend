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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public QuestionResponse updateQuestionByQuestionId(String questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException("Không tìm thấy câu hỏi với ID: " + questionId, HttpStatus.NOT_FOUND));

        question.setContent(request.getContent());
        question.setImage(request.getImage());
        question.setOptions(request.getOptions());
        question.setCorrectAnswerIndex(request.getCorrectAnswerIndex());
        question.setUpdatedAt(now);

        Question updatedQuestion = questionRepository.save(question);

        return mapData.mapOne(updatedQuestion, QuestionResponse.class);
    }

    @Override
    public void deleteQuestionsByQuestionIds(List<String> questionIds) {
        List<Question> questions = questionRepository.findAllById(questionIds);

        if (questions.isEmpty()) {
            throw new CustomException("Không tìm thấy câu hỏi với ID trong danh sách", HttpStatus.NOT_FOUND);
        }

        questionRepository.deleteAll(questions);
    }

    @Override
    public Map<String, Object> getListQuestionsByQuizId(String quizId, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz với ID: " + quizId, HttpStatus.NOT_FOUND));

        Page<Question> questionPage;

        if (search == null || search.isBlank()) {
            questionPage = questionRepository.findByQuiz(quiz, pageable);
        } else {
            questionPage = questionRepository.findByQuizAndContentContainingIgnoreCase(quiz, search, pageable);
        }

        List<QuestionResponse> questionResponses = questionPage.getContent().stream()
                .map(question -> QuestionResponse.builder()
                        .questionId(question.getQuestionId())
                        .content(question.getContent())
                        .image(question.getImage())
                        .options(question.getOptions())
                        .correctAnswerIndex(question.getCorrectAnswerIndex())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", questionPage.getTotalElements());
        response.put("totalPages", questionPage.getTotalPages());
        response.put("currentPage", questionPage.getNumber());
        response.put("questions", questionResponses);

        return response;
    }
}
