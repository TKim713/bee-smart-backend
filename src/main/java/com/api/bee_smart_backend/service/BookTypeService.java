package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.BookTypeRequest;
import com.api.bee_smart_backend.helper.response.BookTypeResponse;
import com.api.bee_smart_backend.model.BookType;

import java.util.List;

public interface BookTypeService {

    List<BookTypeResponse> getAllBookTypes();

    BookTypeResponse createBookType(BookTypeRequest request);

    BookTypeResponse updateBookType(String id, BookTypeRequest request);

    void deleteBookTypeByIds(List<String> bookTypeIds);
}
