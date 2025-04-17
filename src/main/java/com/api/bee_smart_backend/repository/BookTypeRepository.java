package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.BookType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BookTypeRepository extends MongoRepository<BookType, String> {
    Optional<BookType> findByBookNameAndDeletedAtIsNull(String bookName);

    Page<BookType> findByBookNameContainingIgnoreCaseAndDeletedAtIsNull(String search, Pageable pageable);

    Page<BookType> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<BookType> findByBookIdAndDeletedAtIsNull(String bookTypeId);
}
