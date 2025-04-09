package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.QuestionType;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz", HttpStatus.NOT_FOUND));

        Question question = Question.builder()
                .content(request.getContent())
                .image(request.getImage())
                .questionType(QuestionType.valueOf(request.getQuestionType().toUpperCase()))
                .quiz(quiz)
                .createdAt(Instant.now())
                .build();

        updateQuestionFields(question, request);

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
        question.setQuestionType(QuestionType.valueOf(request.getQuestionType().toUpperCase()));
        question.setUpdatedAt(now);

        updateQuestionFields(question, request);

        Question updatedQuestion = questionRepository.save(question);

        return mapData.mapOne(updatedQuestion, QuestionResponse.class);
    }

    @Override
    public void deleteQuestionsByQuestionIds(List<String> questionIds) {
        List<Question> questions = questionRepository.findAllById(questionIds);

        if (questions.isEmpty()) {
            throw new CustomException("Không tìm thấy câu hỏi với ID trong danh sách", HttpStatus.NOT_FOUND);
        }

        for (Question question : questions) {
            question.setDeletedAt(now);
        }

        questionRepository.saveAll(questions);
    }

    @Override
    public QuestionResponse getQuestionByQuestionId(String questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException("Không tìm thấy câu hỏi với ID: " + questionId, HttpStatus.NOT_FOUND));

        return mapData.mapOne(question, QuestionResponse.class);
    }

    @Override
    public Map<String, Object> getListQuestionsByQuizId(String quizId, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Tìm quiz theo quizId
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz với ID: " + quizId, HttpStatus.NOT_FOUND));

        Page<Question> questionPage;

        if (search == null || search.isBlank()) {
            questionPage = questionRepository.findByQuizAndDeletedAtIsNull(quiz, pageable);
        } else {
            questionPage = questionRepository.findByQuizAndContentContainingIgnoreCaseAndDeletedAtIsNull(quiz, search, pageable);
        }

        List<QuestionResponse> questionResponses = questionPage.getContent().stream()
                .map(question -> QuestionResponse.builder()
                        .questionId(question.getQuestionId())
                        .content(question.getContent())
                        .image(question.getImage())
                        .questionType(question.getQuestionType().toString())
                        .options(question.getOptions())
                        .correctAnswer(question.getCorrectAnswer())
                        .correctAnswers(question.getCorrectAnswers())
                        .answers(question.getAnswers())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", questionPage.getTotalElements());
        response.put("quizDuration", quiz.getQuizDuration());
        response.put("totalPages", questionPage.getTotalPages());
        response.put("currentPage", questionPage.getNumber());
        response.put("questions", questionResponses);

        return response;
    }

    @Override
    public Map<String, Object> getQuestionsByQuizId(String quizId) {
        // Tìm quiz theo quizId
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz với ID: " + quizId, HttpStatus.NOT_FOUND));

        List<Question> questions = questionRepository.findByQuizAndDeletedAtIsNull(quiz);

        List<QuestionResponse> questionResponses = questions.stream()
                .map(question -> QuestionResponse.builder()
                        .questionId(question.getQuestionId())
                        .content(question.getContent())
                        .image(question.getImage())
                        .questionType(question.getQuestionType().toString())
                        .options(question.getOptions())
                        .correctAnswer(question.getCorrectAnswer())
                        .correctAnswers(question.getCorrectAnswers())
                        .answers(question.getAnswers())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", questions.size());
        response.put("quizDuration", quiz.getQuizDuration());
        response.put("questions", questionResponses);

        return response;
    }

    private void updateQuestionFields(Question question, QuestionRequest request) {
        QuestionType questionType = QuestionType.valueOf(request.getQuestionType().toUpperCase());

        switch (questionType) {
            case MULTIPLE_CHOICE:
                question.setOptions(request.getOptions());
                question.setCorrectAnswer(request.getCorrectAnswer());
                break;

            case MULTI_SELECT:
                question.setOptions(request.getOptions());
                question.setCorrectAnswers(request.getCorrectAnswers());
                break;

            case FILL_IN_THE_BLANK:
                question.setAnswers(request.getAnswers());
                break;

            default:
                throw new CustomException("Loại câu hỏi không hợp lệ", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public boolean checkAnswer(String questionId, String userAnswer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException("Câu hỏi không tồn tại!", HttpStatus.NOT_FOUND));

        // Nếu là câu hỏi 1 đáp án
        if (question.getCorrectAnswer() != null) {
            return question.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim());
        }

        // Nếu là câu hỏi nhiều đáp án
        if (question.getCorrectAnswers() != null && !question.getCorrectAnswers().isEmpty()) {
            List<String> userAnswers = Arrays.asList(userAnswer.split(",")); // Tách thành danh sách
            return question.getCorrectAnswers().stream()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()) // Đưa về Set để so sánh không phân biệt thứ tự
                    .equals(userAnswers.stream()
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet()));
        }

        return false; // Trường hợp không có đáp án nào
    }
}
