package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson")
public class LessonController {
    @Autowired
    private LessonService lessonService;

//    @GetMapping("/getAll")
//    public ResponseEntity<List<Lesson>> getAllGrades() {
//        List<Grade> grades = lessonService.getAllLessons();
//        if (grades.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(grades);
//    }

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

    @PostMapping
    public ResponseEntity<ResponseObject<LessonResponse>> createLesson(@RequestBody LessonRequest request) {
        try {
            LessonResponse createdLesson = lessonService.createLesson(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lesson created successfully", createdLesson));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error creating lesson: " + e.getMessage(), null));
        }
    }
}
