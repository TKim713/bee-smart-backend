package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.service.ChapterService;
import com.api.bee_smart_backend.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {
    @Autowired
    private ChapterService chapterService;
    @Autowired
    private TopicService topicService;

    @GetMapping("/{chapterId}/topics")
    public ResponseEntity<Map<String, Object>> getTopicsByChapter(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int skip) {

        Map<String, Object> response = topicService.getListTopicByChapter(chapterId, limit, skip);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{chapterId}/topics")
    public ResponseEntity<ResponseObject<TopicResponse>> createTopicByChapterId(
            @PathVariable Long chapterId,
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
}
