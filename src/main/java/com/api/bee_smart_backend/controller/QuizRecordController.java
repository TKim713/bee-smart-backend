package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.QuizRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class QuizRecordController {
    @Autowired
    private QuizRecordService quizRecordService;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getListQuizRecord(@RequestParam(name = "page", required = false) String page,
                                                                                 @RequestParam(name = "size", required = false) String size,
                                                                                 @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = quizRecordService.getListQuizRecord(page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy danh sách lịch sử bài kiểm tra thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

}
