package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
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

    @GetMapping("/getAll")
    public Map<String, Object> getAllLessons(@RequestParam(name = "page", required = false) String page,
                                              @RequestParam(name = "size", required = false) String size,
                                              @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        Map<String, Object> result = lessonService.getAllLessons(page, size, search);

        if (result.isEmpty()) {
            return Collections.emptyMap();
        }
        return result;
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseObject<Lesson>> createLesson(@RequestBody LessonRequest request) {
        try {
            Lesson createdLesson = lessonService.createLesson(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lesson created successfully", createdLesson));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error creating lesson: " + e.getMessage(), null));
        }
    }

}
