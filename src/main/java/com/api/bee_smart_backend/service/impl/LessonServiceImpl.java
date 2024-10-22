package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.model.Lesson;
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

    private LocalDateTime now = LocalDateTime.now();

//    @Override
//    public List<LessonResponse> getListLesson() {
//        List<LessonResponse> lessonList  = lessonRepository.getListLesson();
//        return lessonList.stream().map(Lesson::toLessonResponse).toList();
//    }

    public Map<String, Object> getAllLessons(String page, String size, String search) {
        // Xử lý phân trang
        int pageNumber = (page != null) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Nếu có tìm kiếm, sử dụng search để lọc kết quả
        Page<Lesson> lessons;
        if (search != null && !search.isEmpty()) {
            lessons = lessonRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            lessons = lessonRepository.findAll(pageable);
        }

        // Chuyển đổi từ Lesson sang LessonResponse
        List<LessonResponse> lessonResponses = lessons.getContent().stream().map(lesson ->
                LessonResponse.builder()
                        .lesson_name(lesson.getLesson_name())
                        .description(lesson.getDescription())
                        .content(lesson.getContent())
                        .grade_name(lesson.getGrade().getGrade_name())
                        .topic(lesson.getTopic().getTopic_name())
                        .build()
        ).collect(Collectors.toList());

        // Chuẩn bị kết quả trả về
        Map<String, Object> response = new HashMap<>();
        response.put("lessons", lessonResponses);
        response.put("currentPage", lessons.getNumber());
        response.put("totalItems", lessons.getTotalElements());
        response.put("totalPages", lessons.getTotalPages());

        return response;
    }

    @Override
    public Lesson createLesson(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setLesson_name(request.getLessonName());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
        lesson.setGrade(gradeRepository.findById(request.getGradeId()).orElse(null)); // Lấy Grade
        lesson.setTopic(topicRepository.findById(request.getTopicId()).orElse(null)); // Lấy Topic
        lesson.setCreate_at(Timestamp.valueOf(now));

        return lessonRepository.save(lesson);
    }
}
