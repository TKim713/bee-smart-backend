package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.BookType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookTypeRepository extends MongoRepository<BookType, String> {
    Optional<BookType> findByBookNameAndDeletedAtIsNull(String bookName);

    List<BookType> findAllByDeletedAtIsNull();
}
