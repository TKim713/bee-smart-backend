package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizCountByGradeResponse;
import com.api.bee_smart_backend.helper.response.aggregation.QuizTopicProjection;
import com.api.bee_smart_backend.model.QuizRecord;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.QuizRecordRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.QuizRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

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
