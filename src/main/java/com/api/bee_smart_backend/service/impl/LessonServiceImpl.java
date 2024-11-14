package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.GradeRepository;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
    private GradeRepository gradeRepository;
    @Autowired
    private TopicRepository topicRepository;

    private final MapData mapData;

    private LocalDateTime now = LocalDateTime.now();

    @Override
    public Map<String, Object> getListLessonByTopic(Long topicId, int limit, int skip) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        long total = lessonRepository.countByTopic(topic);

        Pageable pageable = PageRequest.of(skip / limit, limit);
        Page<Lesson> lessonPage = lessonRepository.findByTopic(topic, pageable);

        List<LessonResponse> lessons = lessonPage.stream()
                .map(lesson -> mapData.mapOne(lesson, LessonResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("data", lessons);
        response.put("limit", limit);
        response.put("skip", skip);

        return response;
    }

//    public Map<String, Object> getAllLessons(String page, String size, String search) {
//        // Xử lý phân trang
//        int pageNumber = (page != null) ? Integer.parseInt(page) : 0;
//        int pageSize = (size != null) ? Integer.parseInt(size) : 10;
//
//        Pageable pageable = PageRequest.of(pageNumber, pageSize);
//
//        // Nếu có tìm kiếm, sử dụng search để lọc kết quả
//        Page<Lesson> lessons;
//        if (search != null && !search.isEmpty()) {
//            lessons = lessonRepository.findByNameContainingIgnoreCase(search, pageable);
//        } else {
//            lessons = lessonRepository.findAll(pageable);
//        }
//
//        // Chuyển đổi từ Lesson sang LessonResponse
//        List<LessonResponse> lessonResponses = lessons.getContent().stream().map(lesson ->
//                LessonResponse.builder()
//                        .lesson_name(lesson.getLesson_name())
//                        .description(lesson.getDescription())
//                        .content(lesson.getContent())
//                        .build()
//        ).collect(Collectors.toList());
//
//        // Chuẩn bị kết quả trả về
//        Map<String, Object> response = new HashMap<>();
//        response.put("lessons", lessonResponses);
//        response.put("currentPage", lessons.getNumber());
//        response.put("totalItems", lessons.getTotalElements());
//        response.put("totalPages", lessons.getTotalPages());
//
//        return response;
//    }

    @Override
    public LessonResponse createLessonByTopicId(Long topicId, LessonRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        boolean lessonExists = topic.getLessons().stream()
                .anyMatch(lesson -> lesson.getLesson_name().equalsIgnoreCase(request.getLesson_name()));

        if (lessonExists) {
            throw new CustomException("Bài học '" + request.getLesson_name() + "' đã tồn tại trong chủ đề này.", HttpStatus.CONFLICT);
        }

        Lesson lesson = Lesson.builder()
                .lesson_name(request.getLesson_name())
                .description(request.getDescription())
                .content(request.getContent())
                .topic(topic)
                .create_at(Timestamp.valueOf(now))
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        topic.addLesson(savedLesson);
        topicRepository.save(topic);

        return mapData.mapOne(savedLesson, LessonResponse.class);
    }

    @Override
    public LessonResponse getLessonById(Long lessonId) {
        // Check if the lesson is accessible
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Lesson not found with ID: " + lessonId, HttpStatus.NOT_FOUND));
        if (isFirstLessonOfFirstTopic(lesson) || isAuthenticated()) {
            return mapData.mapOne(lesson, LessonResponse.class);
        } else {
            throw new CustomException("Unauthorized access. Please log in to continue learning.", HttpStatus.UNAUTHORIZED);
        }
    }

    public boolean isFirstLessonOfFirstTopic(Lesson lesson) {
        // Find the first topic in the database
        Topic firstTopic = topicRepository.findFirstByOrderByIdAsc();
        // Find the first lesson of the first topic
        Lesson firstLesson = lessonRepository.findFirstByTopicOrderByIdAsc(firstTopic);
        // Check if the lesson is the first lesson of the first topic
        return lesson.getLesson_id() == firstLesson.getLesson_id();
    }

    @Override
    public boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            return true;
        }
        return false;
    }
}
