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
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getAllSubjects(@RequestParam(name = "page", required = false) String page,
                                                                              @RequestParam(name = "size", required = false) String size,
                                                                              @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = subjectService.getAllSubjects(page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy môn học thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
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

    @GetMapping("/{subjectId}")
    public ResponseEntity<ResponseObject<SubjectResponse>> getSubjectBySubjectId(@PathVariable String subjectId) {
        try {
            SubjectResponse subjectResponse = subjectService.getSubjectBySubjectId(subjectId);
            return ResponseEntity.ok(new ResponseObject<>(
                    HttpStatus.OK.value(),
                    "Lấy môn học thành công",
                    subjectResponse
            ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy môn học: " + e.getMessage(), null));
        }
    }
}
