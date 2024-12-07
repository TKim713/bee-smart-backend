package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.helper.response.QuizCountByGradeResponse;
import com.api.bee_smart_backend.model.QuizRecord;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizRecordRepository extends MongoRepository<QuizRecord, String> {
    List<QuizRecord> findByUser(User user);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'quiz', localField: 'quiz', foreignField: '_id', as: 'quizData' } }",
            "{ $unwind: '$quizData' }",
            "{ $lookup: { from: 'lesson', localField: 'quizData.lesson', foreignField: '_id', as: 'lessonData' } }",
            "{ $unwind: '$lessonData' }",
            "{ $lookup: { from: 'topic', localField: 'lessonData.topic', foreignField: '_id', as: 'topicData' } }",
            "{ $unwind: '$topicData' }",
            "{ $project: { recordId: 1, quizTitle: '$quizData.title', lessonName: '$lessonData.lessonName', topicName: '$topicData.topicName' } }"
    })
    List<QuizCountByGradeResponse> countQuizzesByGrade();
}
