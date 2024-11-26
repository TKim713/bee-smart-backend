package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.ChapterRequest;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.ChapterResponse;
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
            @PathVariable String chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int skip) {

        Map<String, Object> response = topicService.getListTopicByChapter(chapterId, limit, skip);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/grade/{gradeId}")
    public ResponseEntity<ResponseObject<ChapterResponse>> createChapterByGradeId(
            @PathVariable String gradeId,
            @RequestBody ChapterRequest request) {
        try {
            ChapterResponse chapterResponse = chapterService.createChapterByGradeId(gradeId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResponseObject<>(HttpStatus.CREATED.value(), "Tạo chương thành công!", chapterResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi tạo chương: " + e.getMessage(), null));
        }
    }
}
