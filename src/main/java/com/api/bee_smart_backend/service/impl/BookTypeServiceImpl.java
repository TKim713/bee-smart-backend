package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.BookTypeRequest;
import com.api.bee_smart_backend.helper.response.BookTypeResponse;
import com.api.bee_smart_backend.model.BookType;
import com.api.bee_smart_backend.repository.BookTypeRepository;
import com.api.bee_smart_backend.service.BookTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookTypeServiceImpl implements BookTypeService {
    @Autowired
    private final BookTypeRepository bookTypeRepository;
    private final Instant now = Instant.now();
    private final MapData mapData;

    @Override
    public List<BookTypeResponse> getAllBookTypes() {
        List<BookType> bookTypes = bookTypeRepository.findAllByDeletedAtIsNull();
        return bookTypes.stream()
                .map(bookType -> mapData.mapOne(bookType, BookTypeResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public BookTypeResponse createBookType(BookTypeRequest request) {
        bookTypeRepository.findByBookNameAndDeletedAtIsNull(request.getBookName()).ifPresent(existingBookType -> {
            throw new CustomException("Loại sách với tên '" + request.getBookName() + "' đã tồn tại", HttpStatus.CONFLICT);
        });

        BookType bookType = BookType.builder()
                .bookName(request.getBookName())
                .createdAt(now)
                .build();
        bookType = bookTypeRepository.save(bookType);

        return mapData.mapOne(bookType, BookTypeResponse.class);
    }

    @Override
    public BookTypeResponse updateBookType(String id, BookTypeRequest request) {
        BookType bookType = bookTypeRepository.findById(id).orElseThrow(() ->
                new CustomException("Loại sách không tồn tại", HttpStatus.NOT_FOUND));
        bookType.setBookName(request.getBookName());
        bookType.setUpdatedAt(now);
        bookType = bookTypeRepository.save(bookType);

        return mapData.mapOne(bookType, BookTypeResponse.class);
    }

    @Override
    public void deleteBookTypeByIds(List<String> bookTypeIds) {
        List<BookType> bookTypes = bookTypeRepository.findAllById(bookTypeIds);
        if (bookTypes.isEmpty()) {
            throw new CustomException("Không tìm thấy loại sách để xóa", HttpStatus.NOT_FOUND);
        }
        bookTypes.forEach(bookType -> bookType.setDeletedAt(now));
        bookTypeRepository.saveAll(bookTypes);
    }
}
