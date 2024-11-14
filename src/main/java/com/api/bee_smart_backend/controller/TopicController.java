package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.LessonService;
import com.api.bee_smart_backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    @Autowired
    private TopicService topicService;
    @Autowired
    private LessonService lessonService;

    @PostMapping("/{topicId}/lessons")
    public ResponseEntity<ResponseObject<LessonResponse>> createLessonByTopicId(
            @PathVariable Long topicId,
            @RequestBody LessonRequest request) {
        try {
            LessonResponse lessonResponse = lessonService.createLessonByTopicId(topicId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResponseObject<>(HttpStatus.CREATED.value(), "Bài học được tạo thành công!", lessonResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo bài học: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{topicId}/lessons")
    public ResponseEntity<Map<String, Object>> getLessonsByTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "10") int limit, // Default value of 10 for limit
            @RequestParam(defaultValue = "0") int skip) { // Default value of 0 for skip

        Map<String, Object> response = lessonService.getListLessonByTopic(topicId, limit, skip);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
