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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Map<String, Object> getAllBookTypes(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<BookType> bookTypePage;

        if (search == null || search.isBlank()) {
            bookTypePage = bookTypeRepository.findAllByDeletedAtIsNull(pageable);
        } else {
            bookTypePage = bookTypeRepository.findByBookNameContainingIgnoreCaseAndDeletedAtIsNull(search, pageable);
        }

        List<BookTypeResponse> bookTypeResponses = bookTypePage.getContent().stream()
                .map(bookType -> mapData.mapOne(bookType, BookTypeResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", bookTypePage.getTotalElements());
        response.put("totalPages", bookTypePage.getTotalPages());
        response.put("currentPage", bookTypePage.getNumber());
        response.put("bookTypes", bookTypeResponses);

        return response;
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

    @Override
    public BookTypeResponse getBookTypeById(String bookTypeId) {
        BookType bookType = bookTypeRepository.findByBookIdAndDeletedAtIsNull(bookTypeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy loại sách với ID: " + bookTypeId, HttpStatus.NOT_FOUND));
        return mapData.mapOne(bookType, BookTypeResponse.class);
    }

}
