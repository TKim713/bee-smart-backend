package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.GradeResponse;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.ChapterRepository;
import com.api.bee_smart_backend.repository.GradeRepository;
import com.api.bee_smart_backend.repository.LessonRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private LessonRepository lessonRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public List<GradeResponse> getAllGrades() {
        List<Grade> gradeList = gradeRepository.findAll();
        return gradeList.stream()
                .map(grade -> mapData.mapOne(grade, GradeResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public GradeResponse createGrade(GradeRequest request) {
        gradeRepository.findByGradeName(request.getGradeName()).ifPresent(existingGrade -> {
            throw new CustomException("Lớp học với tên '" + request.getGradeName() + "' đã tồn tại", HttpStatus.CONFLICT);
        });

        Grade grade = Grade.builder()
                .gradeName(request.getGradeName())
                .createdAt(now)
                .chapters(new ArrayList<>())
                .build();

        Grade savedGrade = gradeRepository.save(grade);

        return mapData.mapOne(savedGrade, GradeResponse.class);
    }

    @Override
    public Map<String, Object> getLessonsByGrade(String gradeId, int limit, int skip) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Lớp không tồn tại", HttpStatus.NOT_FOUND));

        List<Chapter> chapters = chapterRepository.findByGrade(grade);

        Pageable pageable = PageRequest.of(skip / limit, limit);
        Map<String, Object> response = new HashMap<>();

        List<Topic> topics = chapters.stream()
                .flatMap(chapter -> topicRepository.findByChapter(chapter, pageable).getContent().stream())
                .toList();

        List<LessonResponse> lessonResponses = topics.stream()
                .flatMap(topic -> lessonRepository.findByTopic(topic).stream())
                .map(lesson -> new LessonResponse(
                        lesson.getLessonId(),
                        lesson.getLessonName(),
                        lesson.getDescription(),
                        lesson.getContent()
                ))
                .collect(Collectors.toList());

        long totalTopics = topicRepository.countByChapterIn(chapters);

        response.put("total", totalTopics);
        response.put("data", lessonResponses);
        response.put("limit", limit);
        response.put("skip", skip);

        return response;
    }
}
