package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.request.UserAnswer;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.QuestionResult;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.record.QuizRecord;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {
    @Autowired
    private final TopicRepository topicRepository;
    @Autowired
    private final QuizRepository quizRepository;
    @Autowired
    private final QuestionRepository questionRepository;
    @Autowired
    private final LessonRepository lessonRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final QuizRecordRepository quizRecordRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();
    ZonedDateTime vietnamTime = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    LocalDate today = vietnamTime.toLocalDate();

    @Override
    public QuizResponse getQuizById(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz với ID: " + quizId, HttpStatus.NOT_FOUND));

        return mapData.mapOne(quiz, QuizResponse.class);
    }

    @Override
    public QuizResponse createQuizByLessonId(String lessonId, QuizRequest request) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bài học", HttpStatus.NOT_FOUND));

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .lesson(lesson)
                .image(request.getImage())
                .quizDuration(request.getQuizDuration())
                .questions(new ArrayList<>())
                .createdAt(now)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        return mapData.mapOne(savedQuiz, QuizResponse.class);
    }

    @Override
    public QuizResponse createQuizByTopicId(String topicId, QuizRequest request) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề", HttpStatus.NOT_FOUND));

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(topic)
                .image(request.getImage())
                .quizDuration(request.getQuizDuration())
                .questions(new ArrayList<>())
                .createdAt(now)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        return mapData.mapOne(savedQuiz, QuizResponse.class);
    }

    @Override
    public QuizResponse updateQuiz(String quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz", HttpStatus.NOT_FOUND));

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setImage(request.getImage());
        quiz.setUpdatedAt(Instant.now());

        Quiz updatedQuiz = quizRepository.save(quiz);

        return mapData.mapOne(updatedQuiz, QuizResponse.class);
    }

    @Override
    public List<String> deleteQuizzes(List<String> quizIds) {
        List<String> undeletedQuizIds = new ArrayList<>();

        for (String quizId : quizIds) {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new CustomException("Không tìm thấy quiz: " + quizId, HttpStatus.NOT_FOUND));

            if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                undeletedQuizIds.add(quizId);
                continue;
            }

            quiz.setDeletedAt(now);
            quizRepository.save(quiz);
        }
        return undeletedQuizIds;
    }

    @Override
    public Map<String, Object> getQuizzesByTopic(String topicId, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        List<Lesson> lessons = lessonRepository.findByTopicAndDeletedAtIsNull(topic);

        if (lessons.isEmpty()) {
            return Collections.emptyMap();
        }

        Page<Quiz> quizPage;

        if (search == null || search.isBlank()) {
            quizPage = quizRepository.findByTopicAndDeletedAtIsNull(topic, pageable);
        } else {
            quizPage = quizRepository.findByTopicAndSearchAndDeletedAtIsNull(topic, search, pageable);
        }

        List<QuizResponse> quizResponses = quizPage.getContent().stream()
                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", quizPage.getTotalElements());
        response.put("totalPages", quizPage.getTotalPages());
        response.put("currentPage", quizPage.getNumber());
        response.put("quizzes", quizResponses);

        return response;
    }

    @Override
    public Map<String, Object> getQuizzesByLessonId(String lessonId, String page, String size) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "title"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bài học với ID: " + lessonId, HttpStatus.NOT_FOUND));

        Page<Quiz> quizPage = quizRepository.findByLessonAndDeletedAtIsNull(lesson, pageable);

        List<QuizResponse> quizResponses = quizPage.getContent().stream()
                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", quizPage.getTotalElements());
        response.put("totalPages", quizPage.getTotalPages());
        response.put("currentPage", quizPage.getNumber());
        response.put("quizzes", quizResponses);

        return response;
    }

    @Override
    public Map<String, Object> submitQuiz(String jwtToken, String quizId, SubmissionRequest request, String page, String size) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Không tìm thấy quiz với ID: " + quizId, HttpStatus.NOT_FOUND));

        List<Question> allQuestions = questionRepository.findByQuizAndDeletedAtIsNull(quiz);
        int correctAnswersCount = 0;

        List<QuestionResult> results = new ArrayList<>();
        for (UserAnswer userAnswer : request.getAnswers()) {
            Question question = allQuestions.stream()
                    .filter(q -> q.getQuestionId().equals(userAnswer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Không tìm thấy câu hỏi với ID: " + userAnswer.getQuestionId(), HttpStatus.BAD_REQUEST));

            boolean isCorrect = switch (question.getQuestionType()) {
                case MULTIPLE_CHOICE -> question.getCorrectAnswer().equals(userAnswer.getSelectedAnswer());
                case MULTI_SELECT ->
                        new HashSet<>(question.getCorrectAnswers()).containsAll(userAnswer.getSelectedAnswers())
                                && new HashSet<>(userAnswer.getSelectedAnswers()).containsAll(question.getCorrectAnswers());
                case FILL_IN_THE_BLANK -> question.getAnswers().stream()
                        .anyMatch(answer -> answer.equalsIgnoreCase(userAnswer.getSelectedAnswer()));
                default -> throw new CustomException("Loại câu hỏi không hợp lệ", HttpStatus.BAD_REQUEST);
            };

            if (isCorrect) {
                correctAnswersCount++;
            }

            results.add(QuestionResult.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .image(question.getImage())
                    .options(question.getOptions())
                    .correctAnswer(question.getCorrectAnswer())
                    .userAnswer(userAnswer.getSelectedAnswer())
                    .isCorrect(isCorrect)
                    .build());
        }

        double points = (double) correctAnswersCount / allQuestions.size() * 10;

        int fromIndex = Math.min(pageNumber * pageSize, results.size());
        int toIndex = Math.min(fromIndex + pageSize, results.size());
        List<QuestionResult> paginatedResults = results.subList(fromIndex, toIndex);

        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        QuizRecord quizRecord = QuizRecord.builder()
                .user(user)
                .quiz(quiz)
                .gradeName(quiz.getTopic().getGrade().getGradeName())
                .totalQuestions(allQuestions.size())
                .correctAnswers(correctAnswersCount)
                .points(points)
                .timeSpent(request.getTimeSpent())
                .submitDate(today)
                .createdAt(now)
                .build();

        quizRecordRepository.save(quizRecord);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalQuestions", allQuestions.size());
        response.put("correctAnswers", correctAnswersCount);
        response.put("points", points);
        response.put("quizDuration", quiz.getQuizDuration());
        response.put("totalPages", (int) Math.ceil((double) results.size() / pageSize));
        response.put("currentPage", pageNumber);
        response.put("questions", paginatedResults);

        return response;
    }
}
