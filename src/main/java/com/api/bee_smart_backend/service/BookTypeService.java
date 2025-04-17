package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.BookTypeRequest;
import com.api.bee_smart_backend.helper.response.BookTypeResponse;
import com.api.bee_smart_backend.model.BookType;

import java.util.List;
import java.util.Map;

public interface BookTypeService {

    Map<String, Object> getAllBookTypes(String page, String size, String search);

    BookTypeResponse createBookType(BookTypeRequest request);

    BookTypeResponse updateBookType(String id, BookTypeRequest request);

    void deleteBookTypeByIds(List<String> bookTypeIds);

    BookTypeResponse getBookTypeByBookTypeId(String bookTypeId);
}
