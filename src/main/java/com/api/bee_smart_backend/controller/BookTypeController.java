package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.BookTypeRequest;
import com.api.bee_smart_backend.helper.response.BookTypeResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.BookTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/book-types")
public class BookTypeController {
    @Autowired
    private BookTypeService bookTypeService;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getAllBookTypes(@RequestParam(name = "page", required = false) String page,
                                                                               @RequestParam(name = "size", required = false) String size,
                                                                               @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = bookTypeService.getAllBookTypes(page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy loại sách thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PostMapping
    public ResponseEntity<ResponseObject<BookTypeResponse>> createBookType(@RequestBody BookTypeRequest request) {
        try {
            BookTypeResponse bookType = bookTypeService.createBookType(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo loại sách thành công!", bookType));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo loại sách: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{bookTypeId}")
    public ResponseEntity<ResponseObject<BookTypeResponse>> updateBookType(@PathVariable String bookTypeId, @RequestBody BookTypeRequest request) {
        try {
            BookTypeResponse bookType = bookTypeService.updateBookType(bookTypeId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Cập nhật loại sách thành công!", bookType));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi cập nhật loại sách: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<Void>> deleteBookTypeByIds(@RequestBody List<String> bookTypeIds) {
        try {
            bookTypeService.deleteBookTypeByIds(bookTypeIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Xóa loại sách thành công!", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa loại sách: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{bookTypeId}")
    public ResponseEntity<ResponseObject<BookTypeResponse>> getBookTypeByBookTypeId(@PathVariable String bookTypeId) {
        try {
            BookTypeResponse bookTypeResponse = bookTypeService.getBookTypeByBookTypeId(bookTypeId);
            return ResponseEntity.ok(new ResponseObject<>(
                    HttpStatus.OK.value(),
                    "Lấy loại sách thành công",
                    bookTypeResponse
            ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy loại sách: " + e.getMessage(), null));
        }
    }
}
