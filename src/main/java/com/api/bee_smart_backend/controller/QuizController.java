package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuizRequest;
import com.api.bee_smart_backend.helper.request.SubmissionRequest;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.QuestionService;
import com.api.bee_smart_backend.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuestionService questionService;

    @PostMapping("/lesson/{lessonId}")
    public ResponseEntity<ResponseObject<QuizResponse>> createQuizByLessonId(
            @PathVariable String lessonId,
            @RequestBody QuizRequest request) {
        try {
            QuizResponse response = quizService.createQuizByLessonId(lessonId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo quiz thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo quiz: " + e.getMessage(), null));
        }
    }

    @PostMapping("/topic/{topicId}")
    public ResponseEntity<ResponseObject<QuizResponse>> createQuizByTopicId(
            @PathVariable String topicId,
            @RequestBody QuizRequest request) {
        try {
            QuizResponse response = quizService.createQuizByTopicId(topicId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Tạo quiz thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo quiz: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseObject<QuizResponse>> getQuizById(@PathVariable String quizId) {
        try {
            QuizResponse quizResponse = quizService.getQuizById(quizId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy thông tin quiz thành công!", quizResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy thông tin quiz: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{quizId}")
    public ResponseEntity<ResponseObject<QuizResponse>> updateQuiz(
            @PathVariable String quizId,
            @RequestBody QuizRequest request) {
        try {
            QuizResponse response = quizService.updateQuiz(quizId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Cập nhật quiz thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi cập nhật quiz: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<List<String>>> deleteQuizzes(@RequestBody List<String> quizIds) {
        try {
            List<String> undeletedQuizIds = quizService.deleteQuizzes(quizIds);
            if (undeletedQuizIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseObject<>(HttpStatus.OK.value(), "Xóa quiz thành công!", null));
            } else {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .body(new ResponseObject<>(HttpStatus.PARTIAL_CONTENT.value(),
                                "Không thể xóa một số quiz vì chúng có câu hỏi liên quan.",
                                undeletedQuizIds));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa quiz: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{quizId}/questions")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getListQuestionsByQuizId(
            @PathVariable String quizId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = questionService.getListQuestionsByQuizId(quizId, page, size, search);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy câu hỏi thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{quizId}/submit-quiz")
    public ResponseEntity<ResponseObject<Map<String, Object>>> submitQuiz(
            @RequestHeader("Authorization") String token,
            @PathVariable String quizId,
            @RequestBody SubmissionRequest request,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            Map<String, Object> response = quizService.submitQuiz(jwtToken, quizId, request, page, size);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Nộp bài quiz thành công", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi nộp bài: " + e.getMessage(), null));
        }
    }
}
