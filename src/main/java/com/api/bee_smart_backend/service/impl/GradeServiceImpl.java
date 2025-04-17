package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.GradeResponse;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.repository.GradeRepository;
import com.api.bee_smart_backend.repository.LessonRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    public Grade createGrade(GradeRequest request) {
        gradeRepository.findByGradeNameAndDeletedAtIsNull(request.getGradeName()).ifPresent(existingGrade -> {
            throw new CustomException("Lớp học với tên '" + request.getGradeName() + "' đã tồn tại", HttpStatus.CONFLICT);
        });

        Grade grade = Grade.builder()
                .gradeName(request.getGradeName())
                .createdAt(now)
                .build();

        return gradeRepository.save(grade);
    }
}
