package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.repository.ChapterRepository;
import com.api.bee_smart_backend.helper.request.ChapterRequest;
import com.api.bee_smart_backend.helper.response.ChapterResponse;
import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.repository.GradeRepository;
import com.api.bee_smart_backend.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {
    @Autowired
    private final GradeRepository gradeRepository;
    @Autowired
    private final ChapterRepository chapterRepository;

    private final MapData mapData;
    private LocalDateTime now = LocalDateTime.now();

    @Override
    public ChapterResponse createChapterByGradeId(Long gradeId, ChapterRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy lớp có ID: " + gradeId, HttpStatus.NOT_FOUND));

        boolean chapterExists = grade.getChapters().stream()
                .anyMatch(chapter -> chapter.getChapter_name().equalsIgnoreCase(request.getChapter_name()));

        if (chapterExists) {
            throw new CustomException("'" + request.getChapter_name() + "' đã tồn tại cho lớp này.", HttpStatus.CONFLICT);
        }

        Chapter chapter = Chapter.builder()
                .chapter_name(request.getChapter_name())
                .grade(grade)
                .create_at(Timestamp.valueOf(now))
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);
        grade.addChapter(savedChapter);
        gradeRepository.save(grade);

        return mapData.mapOne(savedChapter, ChapterResponse.class);
    }

    @Override
    public Map<String, Object> getListChapterByGrade(Long gradeId, int limit, int skip) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Lớp với ID'" + gradeId + "' không tồn tại", HttpStatus.NOT_FOUND));

        List<Chapter> chapters = chapterRepository.findByGrade(grade)
                .stream()
                .skip(skip)
                .limit(limit)
                .toList();

        List<ChapterResponse> chapterResponses = chapters.stream()
                .map(chapter -> mapData.mapOne(chapter, ChapterResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", grade.getChapters().size());
        response.put("data", chapterResponses);
        response.put("limit", limit);
        response.put("skip", skip);
        return response;
    }
}
