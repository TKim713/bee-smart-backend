package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.LessonRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private TopicRepository topicRepository;

    private final MapData mapData;

    private final Instant now = Instant.now();

    @Override
    public Map<String, Object> getAllLessons(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Lesson> lessonPage;

        if (search == null || search.isBlank()) {
            lessonPage = lessonRepository.findAll(pageable);
        } else {
            lessonPage = lessonRepository.findByLessonNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }

        List<LessonResponse> lessonResponses = lessonPage.getContent().stream()
                .map(lesson -> LessonResponse.builder()
                        .lessonId(lesson.getLessonId())
                        .lessonName(lesson.getLessonName())
                        .description(lesson.getDescription())
                        .content(lesson.getContent())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", lessonPage.getTotalElements());
        response.put("totalPages", lessonPage.getTotalPages());
        response.put("currentPage", lessonPage.getNumber());
        response.put("lessons", lessonResponses);

        return response;
    }

    @Override
    public Map<String, Object> getListLessonByTopic(String topicId, int limit, int skip) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        long total = lessonRepository.countByTopic(topic);

        Pageable pageable = PageRequest.of(skip / limit, limit);
        Page<Lesson> lessonPage = lessonRepository.findByTopic(topic, pageable);

        List<LessonResponse> lessons = lessonPage.getContent().stream()
                .map(lesson -> mapData.mapOne(lesson, LessonResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("data", lessons);
        response.put("limit", limit);
        response.put("skip", skip);

        return response;
    }

    @Override
    public LessonResponse createLessonByTopicId(String topicId, LessonRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        boolean lessonExists = topic.getLessons().stream()
                .anyMatch(lesson -> lesson.getLessonName().equalsIgnoreCase(request.getLessonName()));

        if (lessonExists) {
            throw new CustomException("Bài học '" + request.getLessonName() + "' đã tồn tại trong chủ đề này.", HttpStatus.CONFLICT);
        }

        Lesson lesson = Lesson.builder()
                .lessonName(request.getLessonName())
                .description(request.getDescription())
                .content(request.getContent())
                .topic(topic)
                .createdAt(now)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        topic.addLesson(savedLesson);
        topicRepository.save(topic);

        return mapData.mapOne(savedLesson, LessonResponse.class);
    }

    @Override
    public LessonResponse getLessonById(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Lesson not found with ID: " + lessonId, HttpStatus.NOT_FOUND));

        if (isFirstLessonOfFirstTopic(lesson) || isAuthenticated()) {
            return mapData.mapOne(lesson, LessonResponse.class);
        } else {
            throw new CustomException("Unauthorized access. Please log in to continue learning.", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isFirstLessonOfFirstTopic(Lesson lesson) {
        Topic firstTopic = topicRepository.findFirstByOrderByIdAsc();
        Lesson firstLesson = lessonRepository.findFirstByTopicOrderByIdAsc(firstTopic);

        return lesson.getLessonId().equals(firstLesson.getLessonId());
    }

    @Override
    public boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails;
    }
}