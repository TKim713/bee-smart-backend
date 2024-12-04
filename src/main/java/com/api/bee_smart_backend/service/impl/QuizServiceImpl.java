package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.request.UserAnswer;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.QuestionResult;
import com.api.bee_smart_backend.model.*;
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
    private final StatisticRepository statisticRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public QuizResponse getQuizById(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Quiz not found with ID: " + quizId, HttpStatus.NOT_FOUND));

        return mapData.mapOne(quiz, QuizResponse.class);
    }

    @Override
    public QuizResponse createQuiz(String lessonId, QuizRequest request) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Lesson not found", HttpStatus.NOT_FOUND));

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
    public QuizResponse updateQuiz(String quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Quiz not found", HttpStatus.NOT_FOUND));

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
                    .orElseThrow(() -> new CustomException("Quiz not found: " + quizId, HttpStatus.NOT_FOUND));

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
            quizPage = quizRepository.findByLessonInAndDeletedAtIsNull(lessons, pageable);
        } else {
            quizPage = quizRepository.findByLessonInAndTitleContainingIgnoreCaseAndDeletedAtIsNull(lessons, search, pageable);
        }

        List<QuizResponse> quizResponses = quizPage.getContent().stream()
                .sorted(Comparator.comparing(quiz -> quiz.getLesson().getLessonNumber()))
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                .toList();

        Map<String, Object> response = new HashMap<>();
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

        Map<String, Object> response = new HashMap<>();
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
                .orElseThrow(() -> new CustomException("Quiz not found with ID: " + quizId, HttpStatus.NOT_FOUND));

        List<Question> allQuestions = questionRepository.findByQuizAndDeletedAtIsNull(quiz);
        int correctAnswersCount = 0;

        List<QuestionResult> results = new ArrayList<>();
        for (UserAnswer userAnswer : request.getAnswers()) {
            Question question = allQuestions.stream()
                    .filter(q -> q.getQuestionId().equals(userAnswer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Question not found with ID: " + userAnswer.getQuestionId(), HttpStatus.BAD_REQUEST));

            boolean isCorrect = question.getCorrectAnswerIndex() == userAnswer.getSelectedAnswerIndex();
            if (isCorrect) {
                correctAnswersCount++;
            }

            results.add(QuestionResult.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .image(question.getImage())
                    .options(question.getOptions())
                    .correctAnswerIndex(question.getCorrectAnswerIndex())
                    .userAnswerIndex(userAnswer.getSelectedAnswerIndex())
                    .isCorrect(isCorrect)
                    .build());
        }

        double points = (double) correctAnswersCount / allQuestions.size() * 10;

        int fromIndex = Math.min(pageNumber * pageSize, results.size());
        int toIndex = Math.min(fromIndex + pageSize, results.size());
        List<QuestionResult> paginatedResults = results.subList(fromIndex, toIndex);

        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Token not found", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        Statistic statistic = statisticRepository.findByUserAndDeletedAtIsNull(user)
                .orElseThrow(() -> new CustomException("Statistic of user not found", HttpStatus.NOT_FOUND));

        statistic.setUpdatedAt(Instant.now());
        statistic.setNumberOfQuestionsAnswered(statistic.getNumberOfQuestionsAnswered() + request.getAnswers().size());
        statistic.setNumberOfQuizzesDone(statistic.getNumberOfQuizzesDone() + 1);
        statistic.setTimeSpentDoingQuizzes(statistic.getTimeSpentDoingQuizzes() + request.getTimeSpent());

        statisticRepository.save(statistic);

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
