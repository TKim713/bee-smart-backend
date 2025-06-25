package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.config.NotificationWebSocketHandler;
import com.api.bee_smart_backend.helper.enums.QuestionType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.request.UserAnswer;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.QuestionResult;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.record.QuizRecord;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import java.util.stream.Collectors;

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
    @Autowired
    private final NotificationRepository notificationRepository;

    @Autowired
    private ApplicationContext applicationContext;
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
                .topic(lesson.getTopic())
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

            // Remove the check that prevents deletion if questions exist
            // Soft-delete associated questions
            List<Question> questions = questionRepository.findByQuizAndDeletedAtIsNull(quiz);
            for (Question question : questions) {
                question.setDeletedAt(now);
                questionRepository.save(question);
            }

            // Soft-delete associated quiz records
            List<QuizRecord> quizRecords = quizRecordRepository.findByQuizAndDeletedAtIsNull(quiz);
            for (QuizRecord quizRecord : quizRecords) {
                quizRecord.setDeletedAt(now);
                quizRecordRepository.save(quizRecord);
            }

            // Soft-delete the quiz itself
            quiz.setDeletedAt(now);
            quizRepository.save(quiz);
        }
        return undeletedQuizIds; // Will be empty since we no longer prevent deletion
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
            quizPage = quizRepository.findByTopicAndLessonIsNullAndDeletedAtIsNull(topic, pageable);
        } else {
            quizPage = quizRepository.findByTopicAndLessonIsNullAndSearchAndDeletedAtIsNull(topic, search, pageable);
        }

        List<QuizResponse> quizResponses = quizPage.getContent().stream()
                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                .toList();

        // Prepare the response
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

        // Create a map of user answers for easier lookup
        Map<String, UserAnswer> userAnswerMap = request.getAnswers().stream()
                .filter(userAnswer -> userAnswer.getQuestionId() != null)
                .collect(Collectors.toMap(UserAnswer::getQuestionId, ua -> ua));

        // Iterate over all questions to ensure every question is included in the results
        for (Question question : allQuestions) {
            UserAnswer userAnswer = userAnswerMap.get(question.getQuestionId());
            boolean isCorrect = false;
            String userAnswerText;
            List<String> userAnswersList = null;

            if (userAnswer != null) {
                // Process the user's answer if it exists
                if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    userAnswerText = userAnswer.getSelectedAnswer();
                    isCorrect = question.getCorrectAnswer().equals(userAnswerText);
                } else if (question.getQuestionType() == QuestionType.MULTI_SELECT) {
                    userAnswerText = null;
                    userAnswersList = userAnswer.getSelectedAnswers();
                    isCorrect = userAnswersList != null &&
                            new HashSet<>(question.getCorrectAnswers()).containsAll(userAnswersList) &&
                            new HashSet<>(userAnswersList).containsAll(question.getCorrectAnswers());
                } else if (question.getQuestionType() == QuestionType.FILL_IN_THE_BLANK) {
                    userAnswerText = userAnswer.getSelectedAnswer();
                    isCorrect = question.getAnswers().stream()
                            .anyMatch(answer -> answer.equalsIgnoreCase(userAnswerText));
                } else {
                    userAnswerText = null;
                }
            } // If userAnswer is null, isCorrect remains false, and userAnswerText/answersList remain null
            else {
                userAnswerText = null;
            }

            if (isCorrect) {
                correctAnswersCount++;
            }

            // Build the QuestionResult for this question
            results.add(QuestionResult.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .image(question.getImage())
                    .options(question.getOptions())
                    .correctAnswer(question.getCorrectAnswer())
                    .correctAnswers(question.getCorrectAnswers() != null ? question.getCorrectAnswers() : question.getAnswers())
                    .answers(userAnswersList)
                    .userAnswer(userAnswerText)
                    .isCorrect(isCorrect)
                    .build());
        }

        double points = Math.round((double) correctAnswersCount / allQuestions.size() * 10);

        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        // Create and save QuizRecord
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
                .questionResults(results) // Store all results
                .build();

        QuizRecord savedQuizRecord = quizRecordRepository.save(quizRecord);

        // Add notification if score is less than 5
        if (points < 5) {
            Notification notification = Notification.builder()
                    .user(user)
                    .title("Điểm số thấp trong bài kiểm tra")
                    .message("Điểm của bạn trong " + quiz.getTitle() + " là " + points + ". Hãy xem lại bài học để cải thiện!")
                    .type("QUIZ")
                    .link("/topics/" + quiz.getTopic().getTopicId() + "/lessons-and-quizzes")
                    .read(false)
                    .createdAt(now)
                    .build();

            notificationRepository.save(notification);

            try {
                NotificationWebSocketHandler notificationHandler = applicationContext.getBean(NotificationWebSocketHandler.class);
                notificationHandler.sendNotification(user.getUserId(), notification);
            } catch (Exception e) {
                log.error("Error sending notification", e);
            }
        }

        // Paginate the results for the response
        int fromIndex = Math.min(pageNumber * pageSize, results.size());
        int toIndex = Math.min(fromIndex + pageSize, results.size());
        List<QuestionResult> paginatedResults = results.subList(fromIndex, toIndex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalQuestions", allQuestions.size());
        response.put("correctAnswers", correctAnswersCount);
        response.put("points", points);
        response.put("quizDuration", quiz.getQuizDuration());
        response.put("totalPages", (int) Math.ceil((double) results.size() / pageSize));
        response.put("currentPage", pageNumber);
        response.put("recordId", savedQuizRecord.getRecordId());
        response.put("questions", paginatedResults);

        return response;
    }

    @Override
    public Map<String, Object> getQuizRecordsByUser(String jwtToken, String page, String size) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !page.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        User user = userRepository.findByUserIdAndDeletedAtIsNull(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        Page<QuizRecord> quizRecordPage = quizRecordRepository.findByUserAndDeletedAtIsNull(user, pageable);

        List<QuizRecordResponse> quizRecordResponses = quizRecordPage.getContent().stream()
                .map(this::mapToQuizRecordResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", quizRecordPage.getTotalElements());
        response.put("totalPages", quizRecordPage.getTotalPages());
        response.put("currentPage", quizRecordPage.getNumber());
        response.put("quizRecords", quizRecordResponses);

        return response;
    }

    @Override
    public QuizRecordResponse getQuizRecordById(String jwtToken, String recordId) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        QuizRecord quizRecord = quizRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException("Không tìm thấy kết quả quiz với ID: " + recordId, HttpStatus.NOT_FOUND));

        boolean isOwner = quizRecord.getUser().getUserId().equals(user.getUserId());
        String userRole = String.valueOf(user.getRole());
        boolean isAdmin = userRole != null && userRole.equalsIgnoreCase("SYSTEM_ADMIN");
        boolean isParent = userRole != null && userRole.equalsIgnoreCase("PARENT");
        if (!isOwner && !isAdmin && !isParent) {
            throw new CustomException("Không có quyền truy cập kết quả quiz này", HttpStatus.FORBIDDEN);
        }

        if (quizRecord.getDeletedAt() != null) {
            throw new CustomException("Kết quả quiz đã bị xóa", HttpStatus.GONE);
        }

        // Map QuizRecord to QuizRecordResponse
        return mapToQuizRecordResponse(quizRecord);
    }

    private QuizRecordResponse mapToQuizRecordResponse(QuizRecord quizRecord) {
        return QuizRecordResponse.builder()
                .recordId(quizRecord.getRecordId())
                .username(quizRecord.getUser().getUsername()) // Assumes User has getUsername()
                .quizName(quizRecord.getQuiz().getTitle()) // Assumes Quiz has getTitle()
                .totalQuestions(quizRecord.getTotalQuestions())
                .correctAnswers(quizRecord.getCorrectAnswers())
                .points(quizRecord.getPoints())
                .timeSpent(quizRecord.getTimeSpent())
                .questionResults(quizRecord.getQuestionResults())
                .createdAt(quizRecord.getCreatedAt())
                .build();
    }
}