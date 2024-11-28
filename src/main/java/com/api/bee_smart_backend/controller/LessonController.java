package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonDetailResponse;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    @Autowired
    private LessonService lessonService;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getAllLessons(@RequestParam(name = "page", required = false) String page,
                                                                             @RequestParam(name = "size", required = false) String size,
                                                                             @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = lessonService.getAllLessons(page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lessons retrieved successfully", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "An unexpected error occurred: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ResponseObject<LessonDetailResponse>> getLessonById(@PathVariable String lessonId) {
        try {
            // Retrieve lesson with access logic
            LessonDetailResponse lessonResponse = lessonService.getLessonById(lessonId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lesson retrieved successfully", lessonResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error retrieving lesson: " + e.getMessage(), null));
        }
    }

    @PostMapping("/topic/{topicId}")
    public ResponseEntity<ResponseObject<LessonDetailResponse>> createLessonByTopicId(
            @PathVariable String topicId,
            @RequestBody LessonRequest request) {
        try {
            LessonDetailResponse lessonResponse = lessonService.createLessonByTopicId(topicId, request);
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

    @PutMapping("/{lessonId}/topic/{topicId}")
    public ResponseEntity<ResponseObject<LessonDetailResponse>> updateLessonByTopicId(
            @PathVariable String topicId,
            @PathVariable String lessonId,
            @RequestBody LessonRequest request) {
        try {
            LessonDetailResponse lessonResponse = lessonService.updateLessonByTopicId(topicId, lessonId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Bài học được cập nhật thành công!", lessonResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi cập nhật bài học: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{lessonId}/topic/{topicId}")
    public ResponseEntity<ResponseObject<Void>> deleteLessonByTopicId(
            @PathVariable String topicId,
            @PathVariable String lessonId) {
        try {
            lessonService.deleteLessonByTopicId(topicId, lessonId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ResponseObject<>(HttpStatus.NO_CONTENT.value(), "Bài học đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa bài học: " + e.getMessage(), null));
        }
    }
}
