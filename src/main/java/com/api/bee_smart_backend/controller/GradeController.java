package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/grade")
public class GradeController {
    @Autowired
    private GradeService gradeService;

    @GetMapping
    public ResponseEntity<List<Grade>> getAllGrades() {
        List<Grade> grades = gradeService.getAllGrades();
        if (grades.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
        }
        return ResponseEntity.status(HttpStatus.OK).body(grades);
    }
}
