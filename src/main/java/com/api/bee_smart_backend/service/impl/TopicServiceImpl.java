package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.ChapterRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    @Autowired
    private final ChapterRepository chapterRepository;
    @Autowired
    private final TopicRepository topicRepository;

    private final Instant now = Instant.now();
    private final MapData mapData;

    @Override
    public TopicResponse createTopicByChapterId(String chapterId, TopicRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chương với ID: " + chapterId, HttpStatus.NOT_FOUND));

        Topic topic = Topic.builder()
                .topicName(request.getTopicName())
                .chapter(chapter)
                .createdAt(now)
                .build();

        Topic savedTopic = topicRepository.save(topic);
        chapter.addTopic(savedTopic);
        chapterRepository.save(chapter);

        return mapData.mapOne(savedTopic, TopicResponse.class);
    }

    @Override
    public Map<String, Object> getListTopicByChapter(String chapterId, int limit, int skip) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chương với ID: " + chapterId, HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(skip / limit, limit);
        Page<Topic> topicPage = topicRepository.findByChapter(chapter, pageable);

        List<TopicResponse> topicResponses = topicPage.getContent().stream()
                .map(topic -> mapData.mapOne(topic, TopicResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        long totalTopics = topicRepository.countByChapterIn(List.of(chapter));

        response.put("total", totalTopics);
        response.put("data", topicResponses);
        response.put("limit", limit);
        response.put("skip", skip);

        return response;
    }
}

