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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private LocalDateTime now = LocalDateTime.now();
    private final MapData mapData;

    @Override
    public List<GradeResponse> getAllGrades() {
        List<Grade> gradeList = gradeRepository.findAll();
        return gradeList.stream()
                .map(grade -> mapData.mapOne(grade, GradeResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public GradeResponse createGrade(GradeRequest request) {
        gradeRepository.findByGradeName(request.getGrade_name()).ifPresent(existingGrade -> {
            throw new CustomException("Lớp học với tên '" + request.getGrade_name() + "' đã tồn tại", HttpStatus.CONFLICT);
        });

        Grade grade = Grade.builder()
                .grade_name(request.getGrade_name())
                .create_at(Timestamp.valueOf(now))
                .chapters(new ArrayList<>())
                .build();

        Grade savedGrade = gradeRepository.save(grade);

        return mapData.mapOne(savedGrade, GradeResponse.class);
    }

    @Override
    public List<LessonResponse> getLessonsByGrade(Long gradeId) {
        // Step 1: Find the grade by ID
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Lớp không tồn tại", HttpStatus.NOT_FOUND));

        // Step 2: Find all chapters associated with this grade
        List<Chapter> chapters = chapterRepository.findByGrade(grade);

        // Step 3: Retrieve all topics within each chapter
        List<Topic> topics = chapters.stream()
                .flatMap(chapter -> topicRepository.findByChapter(chapter).stream())
                .toList();

        // Step 4: Retrieve all lessons within each topic and map them to LessonResponse

        return topics.stream()
                .flatMap(topic -> lessonRepository.findByTopic(topic).stream())
                .map(lesson -> new LessonResponse(
                        lesson.getLesson_id(),
                        lesson.getLesson_name(),
                        lesson.getDescription(),
                        lesson.getContent()
                ))
                .collect(Collectors.toList());
    }
}
