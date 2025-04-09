package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Question;
import com.api.bee_smart_backend.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Set;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByQuizAndDeletedAtIsNull(Quiz quiz);

    Page<Question> findByQuizAndDeletedAtIsNull(Quiz quiz, Pageable pageable);

    Page<Question> findByQuizAndContentContainingIgnoreCaseAndDeletedAtIsNull(Quiz quiz, String content, Pageable pageable);

    List<Question> findByTopicsInAndDeletedAtIsNull(List<String> topicIds);

    List<Question> findByTopicsInAndQuestionIdNotInAndDeletedAtIsNull(List<String> topicIds, Set<String> excludeIds);
}
