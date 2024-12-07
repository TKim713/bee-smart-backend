package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.aggregation.QuizTopicProjection;
import com.api.bee_smart_backend.model.QuizRecord;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.QuizRecordRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.QuizRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuizRecordServiceImpl implements QuizRecordService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final QuizRecordRepository quizRecordRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Map<String, Object> getListQuizRecord(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<QuizRecord> quizRecordPage;

        if (search == null || search.isBlank()) {
            quizRecordPage = quizRecordRepository.findAll(pageable);
        } else {
            quizRecordPage = quizRecordRepository.findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(search, search, pageable);
        }

        List<QuizRecordResponse> quizRecordResponses = quizRecordPage.getContent().stream()
                .map(quizRecord -> QuizRecordResponse.builder()
                        .recordId(quizRecord.getRecordId())
                        .username(quizRecord.getUser().getUsername())
                        .quizName(quizRecord.getQuiz().getTitle())
                        .totalQuestions(quizRecord.getTotalQuestions())
                        .correctAnswers(quizRecord.getCorrectAnswers())
                        .points(quizRecord.getPoints())
                        .timeSpent(quizRecord.getTimeSpent())
                        .createdAt(quizRecord.getCreatedAt())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", quizRecordPage.getTotalElements());
        response.put("totalPages", quizRecordPage.getTotalPages());
        response.put("currentPage", quizRecordPage.getNumber());
        response.put("quizRecords", quizRecordResponses);

        return response;
    }

    @Override
    public List<QuizRecord> getQuizRecords(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        return quizRecordRepository.findByUser(user);
    }

    public List<QuizTopicProjection> getQuizTopicProjection() {
        // Create an aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.lookup("quiz", "quiz", "_id", "quizData"), // Lookup from quiz collection
                Aggregation.unwind("quizData"), // Unwind the quizData array
                Aggregation.lookup("lesson", "quizData.lesson", "_id", "lessonData"), // Lookup from lesson collection
                Aggregation.unwind("lessonData"), // Unwind the lessonData array
                Aggregation.lookup("topic", "lessonData.topic", "_id", "topicData"), // Lookup from topic collection
                Aggregation.unwind("topicData"), // Unwind the topicData array
                Aggregation.project("recordId", "quizData.title", "lessonData.lessonName", "topicData.topicName") // Select the fields you want
        );

        // Execute the aggregation query
        return mongoTemplate.aggregate(aggregation, QuizRecord.class, QuizTopicProjection.class).getMappedResults();
    }
}
