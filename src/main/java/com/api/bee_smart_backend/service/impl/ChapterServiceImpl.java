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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final Instant now = Instant.now();

    @Override
    public ChapterResponse createChapterByGradeId(String gradeId, ChapterRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy lớp có ID: " + gradeId, HttpStatus.NOT_FOUND));

        boolean chapterExists = chapterRepository.findByGrade(grade).stream()
                .anyMatch(chapter -> chapter.getChapterName().equalsIgnoreCase(request.getChapterName()));

        if (chapterExists) {
            throw new CustomException("'" + request.getChapterName() + "' đã tồn tại cho lớp này.", HttpStatus.CONFLICT);
        }

        Chapter chapter = Chapter.builder()
                .chapterName(request.getChapterName())
                .grade(grade)
                .createdAt(now)
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);
        grade.addChapter(savedChapter);
        gradeRepository.save(grade);

        return mapData.mapOne(savedChapter, ChapterResponse.class);
    }

    @Override
    public Map<String, Object> getListChapterByGrade(String gradeId, int limit, int skip) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Lớp với ID '" + gradeId + "' không tồn tại", HttpStatus.NOT_FOUND));

        List<Chapter> chapters = chapterRepository.findByGrade(grade);

        List<ChapterResponse> chapterResponses = chapters.stream()
                .skip(skip)
                .limit(limit)
                .map(chapter -> mapData.mapOne(chapter, ChapterResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("total", chapters.size());
        response.put("data", chapterResponses);
        response.put("limit", limit);
        response.put("skip", skip);
        return response;
    }
}
