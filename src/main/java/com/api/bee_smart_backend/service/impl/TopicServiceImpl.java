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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    @Autowired
    private final ChapterRepository chapterRepository;
    @Autowired
    private final TopicRepository topicRepository;

    private LocalDateTime now = LocalDateTime.now();
    private final MapData mapData;

    @Override
    public TopicResponse createTopicByChapterId(Long chapterId, TopicRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chương với ID: " + chapterId, HttpStatus.NOT_FOUND));

        Topic topic = Topic.builder()
                .topic_name(request.getTopic_name())
                .chapter(chapter)
                .create_at(Timestamp.valueOf(now))
                .build();

        Topic savedTopic = topicRepository.save(topic);
        chapter.addTopic(savedTopic);
        chapterRepository.save(chapter);

        return mapData.mapOne(savedTopic, TopicResponse.class);
    }

    @Override
    public Map<String, Object> getListTopicByChapter(Long chapterId, int limit, int skip) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chương với ID: " + chapterId, HttpStatus.NOT_FOUND));

        List<Topic> topics = topicRepository.findByChapter(chapter)
                .stream()
                .skip(skip)
                .limit(limit)
                .toList();

        List<TopicResponse> topicResponses = topics.stream()
                .map(topic -> mapData.mapOne(topic, TopicResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", topicRepository.countByChapter(chapter));
        response.put("data", topicResponses);
        response.put("limit", limit);
        response.put("skip", skip);

        return response;
    }
}
