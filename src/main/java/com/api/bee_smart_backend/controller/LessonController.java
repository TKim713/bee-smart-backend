package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.LessonService;
import com.api.bee_smart_backend.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    @Autowired
    private LessonService lessonService;
    @Autowired
    private QuizService quizService;

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
    public ResponseEntity<ResponseObject<LessonResponse>> getLessonById(@PathVariable String lessonId) {
        try {
            // Retrieve lesson with access logic
            LessonResponse lessonResponse = lessonService.getLessonById(lessonId);
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
    public ResponseEntity<ResponseObject<LessonResponse>> createLessonByTopicId(
            @PathVariable String topicId,
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

    @PutMapping("/{lessonId}")
    public ResponseEntity<ResponseObject<LessonResponse>> updateLessonById(
            @PathVariable String lessonId,
            @RequestBody LessonRequest request) {
        try {
            LessonResponse lessonResponse = lessonService.updateLessonById(lessonId, request);
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

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<ResponseObject<Object>> deleteLessonById(
            @PathVariable String lessonId) {
        try {
            lessonService.deleteLessonById(lessonId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Bài học đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa bài học: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<Object>> deleteLessons(@RequestBody List<String> lessonIds) {
        try {
            lessonService.deleteLessonsByIds(lessonIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Các bài học đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa bài học: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{lessonId}/quizzes")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getQuizzesByLessonId(
            @PathVariable String lessonId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size) {
        try {
            Map<String, Object> result = quizService.getQuizzesByLessonId(lessonId, page, size);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Quizzes được lấy thành công: ", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }
}
