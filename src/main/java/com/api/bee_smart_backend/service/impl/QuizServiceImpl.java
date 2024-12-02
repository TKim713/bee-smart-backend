package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CreateQuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.request.UserAnswer;
import com.api.bee_smart_backend.helper.response.CreateQuizResponse;
import com.api.bee_smart_backend.helper.response.QuestionResult;
import com.api.bee_smart_backend.helper.response.SubmissionResponse;
import com.api.bee_smart_backend.model.Question;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.QuestionRepository;
import com.api.bee_smart_backend.repository.QuizRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuestionRepository questionRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public CreateQuizResponse createQuiz(String topicId, CreateQuizRequest request) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Topic not found", HttpStatus.NOT_FOUND));

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(topic)
                .image(request.getImage())
                .questions(new ArrayList<>())
                .createdAt(now)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        return mapData.mapOne(savedQuiz, CreateQuizResponse.class);
    }

    @Override
    public SubmissionResponse submitQuiz(String quizId, SubmissionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new CustomException("Quiz not found with ID: " + quizId, HttpStatus.NOT_FOUND));

        List<Question> questions = questionRepository.findByQuiz(quiz);

        int correctAnswersCount = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (UserAnswer userAnswer : request.getAnswers()) {
            Question question = questions.stream()
                    .filter(q -> q.getQuestionId().equals(userAnswer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Question not found with ID: " + userAnswer.getQuestionId(), HttpStatus.BAD_REQUEST));

            boolean isCorrect = question.getCorrectAnswerIndex() == userAnswer.getSelectedAnswerIndex();
            if (isCorrect) {
                correctAnswersCount++;
            }

            QuestionResult result = QuestionResult.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .image(question.getImage())
                    .options(question.getOptions())
                    .correctAnswerIndex(question.getCorrectAnswerIndex())
                    .userAnswerIndex(userAnswer.getSelectedAnswerIndex())
                    .isCorrect(isCorrect)
                    .build();

            results.add(result);
        }

        double points = (double) correctAnswersCount / questions.size() * 10;

        return SubmissionResponse.builder()
                .totalQuestions(questions.size())
                .correctAnswers(correctAnswersCount)
                .points(points)
                .results(results)
                .build();
    }
}
