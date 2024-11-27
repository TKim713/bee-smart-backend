package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.*;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.service.GradeService;
import com.api.bee_smart_backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
public class GradeController {
    @Autowired
    private GradeService gradeService;
    @Autowired
    private TopicService topicService;

    @GetMapping
    public ResponseEntity<List<GradeResponse>> getAllGrades() {
        List<GradeResponse> grades = gradeService.getAllGrades();
        if (grades.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
        }
        return ResponseEntity.status(HttpStatus.OK).body(grades);
    }

    @PostMapping
    public ResponseEntity<ResponseObject<Grade>> createGrade(@RequestBody GradeRequest request) {
        try {
            Grade grade = gradeService.createGrade(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo lớp học thành công!", grade));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo lớp: " + e.getMessage(), null));
        }
    }
}
