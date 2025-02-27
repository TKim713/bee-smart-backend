package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.SubjectRequest;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.SubjectResponse;
import com.api.bee_smart_backend.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        List<SubjectResponse> subjects = subjectService.getAllSubjects();
        if (subjects.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
        }
        return ResponseEntity.status(HttpStatus.OK).body(subjects);
    }

    @PostMapping
    public ResponseEntity<ResponseObject<SubjectResponse>> createSubject(@RequestBody SubjectRequest request) {
        try {
            SubjectResponse subject = subjectService.createSubject(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo môn học thành công!", subject));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo môn học: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{subjectId}")
    public ResponseEntity<ResponseObject<SubjectResponse>> updateSubject(@PathVariable String subjectId, @RequestBody SubjectRequest request) {
        try {
            SubjectResponse subject = subjectService.updateSubject(subjectId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Cập nhật môn học thành công!", subject));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi cập nhật môn học: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<Void>> deleteSubjectByIds(@RequestBody List<String> subjectIds) {
        try {
            subjectService.deleteSubjectByIds(subjectIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Xóa môn học thành công!", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa môn học: " + e.getMessage(), null));
        }
    }
}
