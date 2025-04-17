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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
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
    private TopicRepository topicRepository;
    @Autowired
    private LessonRepository lessonRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public Map<String, Object> getAllGrades(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Grade> gradePage;

        if (search == null || search.isBlank()) {
            gradePage = gradeRepository.findAllByDeletedAtIsNull(pageable);
        } else {
            gradePage = gradeRepository.findByGradeNameContainingIgnoreCaseAndDeletedAtIsNull(search, pageable);
        }

        List<GradeResponse> gradeResponses = gradePage.getContent().stream()
                .map(grade -> mapData.mapOne(grade, GradeResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", gradePage.getTotalElements());
        response.put("totalPages", gradePage.getTotalPages());
        response.put("currentPage", gradePage.getNumber());
        response.put("grades", gradeResponses);

        return response;
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

    @Override
    public GradeResponse updateGradeById(String gradeId, GradeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Khối học không tồn tại", HttpStatus.NOT_FOUND));

        grade.setGradeName(request.getGradeName());
        grade.setUpdatedAt(now);
        grade = gradeRepository.save(grade);

        return mapData.mapOne(grade, GradeResponse.class);
    }

    @Override
    public void deleteGradeByIds(List<String> gradeIds) {
        List<Grade> grades = gradeRepository.findAllById(gradeIds);
        if (grades.isEmpty()) {
            throw new CustomException("Không tìm thấy khối học để xóa", HttpStatus.NOT_FOUND);
        }

        grades.forEach(grade -> grade.setDeletedAt(now));
        gradeRepository.saveAll(grades);
    }
}
