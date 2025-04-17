package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.service.LessonService;
import com.api.bee_smart_backend.service.QuizService;
import com.api.bee_smart_backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    @Autowired
    private TopicService topicService;
    @Autowired
    private LessonService lessonService;
    @Autowired
    private QuizService quizService;

    @PostMapping("/grade/{gradeId}/subject/{subjectId}/bookType/{bookTypeId}")
    public ResponseEntity<ResponseObject<TopicResponse>> createTopicByGradeIdSubjectIdAndBookTypeId(
            @PathVariable String gradeId,
            @PathVariable String subjectId,
            @PathVariable String bookTypeId,
            @RequestBody TopicRequest request) {
        try {
            TopicResponse response = topicService.createTopicByGradeIdSubjectIdAndBookTypeId(gradeId, subjectId, bookTypeId, request);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo chủ đề thành công", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo dữ liệu: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{topicId}")
    public ResponseEntity<ResponseObject<TopicResponse>> updateTopic(
            @PathVariable String topicId,
            @RequestBody TopicRequest request) {
        try {
            TopicResponse topicResponse = topicService.updateTopicById(topicId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Chủ đề được cập nhật thành công!", topicResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi cập nhật chủ đề: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{topicId}")
    public ResponseEntity<ResponseObject<Object>> deleteTopicById(@PathVariable String topicId) {
        try {
            topicService.deleteTopicById(topicId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Chủ đề đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa chủ đề: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getTopicsBySubjectGradeAndSemester(
            @RequestParam String subject,
            @RequestParam String grade,
            @RequestParam String semester,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String bookType) { // Thêm bookType
        try {
            Map<String, Object> result = topicService.getTopicsBySubjectGradeAndSemester(subject, grade, semester, page, size, search, bookType);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy chủ đề thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{topicId}/lessons")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getLessonsByTopic(
            @PathVariable String topicId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = lessonService.getListLessonByTopic(topicId, page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy bài học thành công: ", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{topicId}/lessons-and-quizzes")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getLessonsAndQuizzesByTopic(
            @PathVariable String topicId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = lessonService.getLessonsAndQuizzesByTopic(topicId, page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy bài học và quiz thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{topicId}/quizzes")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getQuizzesByTopic(
            @PathVariable String topicId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = quizService.getQuizzesByTopic(topicId, page, size, search);

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

    @GetMapping("/{topicId}")
    public ResponseEntity<ResponseObject<TopicResponse>> getTopicById(@PathVariable String topicId) {
        try {
            TopicResponse topicResponse = topicService.getTopicById(topicId);
            return ResponseEntity.ok(new ResponseObject<>(
                    HttpStatus.OK.value(),
                    "Lấy chủ đề thành công",
                    topicResponse
            ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy chủ đề: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<Object>> deleteTopics(@RequestBody List<String> topicIds) {
        try {
            topicService.deleteTopicsByIds(topicIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Các chủ đề đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa chủ đề: " + e.getMessage(), null));
        }
    }
}
