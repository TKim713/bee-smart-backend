package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.QuestionRequest;
import com.api.bee_smart_backend.helper.response.QuestionResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.QuestionService;
import com.api.bee_smart_backend.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuizService quizService;
    @Autowired
    private QuestionService questionService;

    @PostMapping("/quiz/{quizId}")
    public ResponseEntity<ResponseObject<QuestionResponse>> createQuestion(
            @PathVariable String quizId,
            @RequestBody QuestionRequest request) {
        try {
            QuestionResponse response = questionService.addQuestionToQuiz(quizId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Thêm câu hỏi thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi thêm câu hỏi: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<ResponseObject<QuestionResponse>> updateQuestionByQuestionId(
            @PathVariable String questionId,
            @RequestBody QuestionRequest request) {
        try {
            QuestionResponse response = questionService.updateQuestionByQuestionId(questionId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Cập nhật câu hỏi thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi cập nhật câu hỏi: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<String>> deleteQuestionsByQuestionIds(
            @RequestBody List<String> questionIds) {
        try {
            questionService.deleteQuestionsByQuestionIds(questionIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Xóa câu hỏi thành công!", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi xóa câu hỏi: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<ResponseObject<QuestionResponse>> getQuestionByQuestionId(@PathVariable String questionId) {
        try {
            QuestionResponse response = questionService.getQuestionByQuestionId(questionId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy câu hỏi thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy câu hỏi: " + e.getMessage(), null));
        }
    }
}
