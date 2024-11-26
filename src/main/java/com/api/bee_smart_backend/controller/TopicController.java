package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.service.LessonService;
import com.api.bee_smart_backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    @Autowired
    private TopicService topicService;
    @Autowired
    private LessonService lessonService;

    @PostMapping("/chapter/{chapterId}")
    public ResponseEntity<ResponseObject<TopicResponse>> createTopicByChapterId(
            @PathVariable String chapterId,
            @RequestBody TopicRequest request) {
        try {
            TopicResponse response = topicService.createTopicByChapterId(chapterId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Topic created successfully", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error creating lesson: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{topicId}/lessons")
    public ResponseEntity<Map<String, Object>> getLessonsByTopic(
            @PathVariable String topicId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int skip) {

        Map<String, Object> response = lessonService.getListLessonByTopic(topicId, limit, skip);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
