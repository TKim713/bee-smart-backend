package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Question;
import com.api.bee_smart_backend.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByQuizAndDeletedAtIsNull(Quiz quiz);

    Page<Question> findByQuizAndDeletedAtIsNull(Quiz quiz, Pageable pageable);

    Page<Question> findByQuizAndContentContainingIgnoreCaseAndDeletedAtIsNull(Quiz quiz, String content, Pageable pageable);
}
