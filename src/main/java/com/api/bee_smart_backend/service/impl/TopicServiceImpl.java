package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.TopicLessonResponse;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.GradeRepository;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    @Autowired
    private final GradeRepository gradeRepository;
    @Autowired
    private final TopicRepository topicRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public Map<String, Object> getTopicsByGradeAndSemester(String gradeId, String semester, String page, String size) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Retrieve topics filtered by grade and semester
        Page<Topic> topicPage = topicRepository.findByGrade_GradeIdAndSemester(gradeId, semester, pageable);

        // Map the topics to TopicResponse
        List<TopicLessonResponse> topics = topicPage.getContent().stream()
                .map(topic -> TopicLessonResponse.builder()
                        .topicId(topic.getTopicId())
                        .topicName(topic.getTopicName())
                        .topicNumber(topic.getTopicNumber())
                        .chapter(topic.getChapter())
                        .lessons(topic.getLessons().stream()
                                .map(lesson -> LessonResponse.builder()
                                        .lessonId(lesson.getLessonId())
                                        .lessonName(lesson.getLessonName())
                                        .lessonNumber(lesson.getLessonNumber())
                                        .description(lesson.getDescription())
                                        .content(lesson.getContent())
                                        .build())
                                .toList())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", topicPage.getTotalElements());
        response.put("totalPages", topicPage.getTotalPages());
        response.put("currentPage", topicPage.getNumber());
        response.put("topics", topics);

        return response;
    }

    @Override
    public TopicResponse createTopicByGradeId(String gradeId, TopicRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy khối với ID: " + gradeId, HttpStatus.NOT_FOUND));

        Topic topic = Topic.builder()
                .topicName(request.getTopicName())
                .topicNumber(request.getTopicNumber())
                .chapter(request.getChapter())
                .grade(grade)
                .semester(request.getSemester())
                .createdAt(now)
                .build();

        Topic savedTopic = topicRepository.save(topic);
        grade.getTopics().add(savedTopic);
        gradeRepository.save(grade);

        return mapData.mapOne(savedTopic, TopicResponse.class);
    }
}

