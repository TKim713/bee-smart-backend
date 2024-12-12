package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.model.record.LessonRecord;
import com.api.bee_smart_backend.repository.LessonRepository;
import com.api.bee_smart_backend.repository.LessonRecordRepository;
import com.api.bee_smart_backend.repository.QuizRepository;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private LessonRecordRepository lessonRecordRepository;

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
            lessonPage = lessonRepository.findByLessonNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndDeletedAtIsNull(search, search, pageable);
        }

        List<LessonResponse> lessonResponses = lessonPage.getContent().stream()
                .map(lesson -> LessonResponse.builder()
                        .lessonId(lesson.getLessonId())
                        .lessonName(lesson.getLessonName())
                        .lessonNumber(lesson.getLessonNumber())
                        .description(lesson.getDescription())
                        .content(lesson.getContent())
                        .viewCount(lesson.getViewCount())
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
    public Map<String, Object> getListLessonByTopic(String topicId, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        String semester = topic.getSemester();
        String chapter = switch (semester) {
            case "Học kì 1" -> "I";
            case "Học kì 2" -> "II";
            default -> "Unknown";
        };

        Page<Lesson> lessonPage;

        // Check if search is empty or not
        if (search == null || search.isBlank()) {
            lessonPage = lessonRepository.findByTopicAndDeletedAtIsNull(topic, pageable);
        } else {
            lessonPage = lessonRepository.findByTopicAndSearchAndDeletedAtIsNull(topic, search, pageable);
        }

        List<LessonResponse> lessonResponses = lessonPage.getContent().stream()
                .map(lesson -> {
                    String formattedLessonName = String.format(
                            "%s.%d.%d. %s",
                            chapter,
                            topic.getTopicNumber(),
                            lesson.getLessonNumber(),
                            lesson.getLessonName()
                    );

                    return LessonResponse.builder()
                            .lessonId(lesson.getLessonId())
                            .lessonName(formattedLessonName)
                            .description(lesson.getDescription())
                            .content(lesson.getContent())
                            .viewCount(lesson.getViewCount())
                            .build();
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", lessonPage.getTotalElements());
        response.put("totalPages", lessonPage.getTotalPages());
        response.put("currentPage", lessonPage.getNumber());
        response.put("lessons", lessonResponses);

        return response;
    }

    @Override
    public Map<String, Object> getLessonsAndQuizzesByTopic(String topicId, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        String semester = topic.getSemester();
        String chapter = switch (semester) {
            case "Học kì 1" -> "I";
            case "Học kì 2" -> "II";
            default -> "Unknown";
        };

        Page<Lesson> lessonPage;
        if (search == null || search.isBlank()) {
            lessonPage = lessonRepository.findByTopicAndDeletedAtIsNull(topic, pageable);
        } else {
            lessonPage = lessonRepository.findByTopicAndSearchAndDeletedAtIsNull(topic, search, pageable);
        }

        List<LessonResponse> lessonResponses = lessonPage.getContent().stream()
                .map(lesson -> {
                    String formattedLessonName = String.format(
                            "%s.%d.%d. %s",
                            chapter,
                            topic.getTopicNumber(),
                            lesson.getLessonNumber(),
                            lesson.getLessonName()
                    );

                    return LessonResponse.builder()
                            .lessonId(lesson.getLessonId())
                            .lessonName(formattedLessonName)
                            .description(lesson.getDescription())
                            .content(lesson.getContent())
                            .viewCount(lesson.getViewCount())
                            .build();
                })
                .toList();

        Page<Quiz> quizPage;
        if (search == null || search.isBlank()) {
            quizPage = quizRepository.findByTopicAndLessonIsNullAndDeletedAtIsNull(topic, pageable);
        } else {
            quizPage = quizRepository.findByTopicAndLessonIsNullAndSearchAndDeletedAtIsNull(topic, search, pageable);
        }

        List<QuizResponse> quizResponses = quizPage.getContent().stream()
                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                .toList();

        long totalItems = lessonPage.getTotalElements() + quizPage.getTotalElements();
        int totalPages = Math.max(lessonPage.getTotalPages(), quizPage.getTotalPages());
        int currentPage = Math.max(lessonPage.getNumber(), quizPage.getNumber());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        response.put("currentPage", currentPage);
        response.put("lessons", lessonResponses);
        response.put("quizzes", quizResponses);

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
                .lessonNumber(request.getLessonNumber())
                .description(request.getDescription())
                .content(request.getContent())
                .viewCount(0)
                .topic(topic)
                .createdAt(now)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        topic.addLesson(savedLesson);
        topicRepository.save(topic);

        return mapData.mapOne(savedLesson, LessonResponse.class);
    }

    @Override
    public LessonResponse updateLessonById(String lessonId, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bài học với ID: " + lessonId, HttpStatus.NOT_FOUND));

        lesson.setLessonName(request.getLessonName());
        lesson.setLessonNumber(request.getLessonNumber());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
        lesson.setUpdatedAt(now);

        Lesson updatedLesson = lessonRepository.save(lesson);

        return mapData.mapOne(updatedLesson, LessonResponse.class);
    }

    @Override
    public void deleteLessonById(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bài học với ID: " + lessonId, HttpStatus.NOT_FOUND));

        Topic topic = lesson.getTopic();

        if (topic != null) {
            topic.getLessons().removeIf(existingLesson -> existingLesson.getLessonId().equals(lessonId));
            topicRepository.save(topic);
        }

        lesson.setDeletedAt(now);
        lessonRepository.save(lesson);
    }

    @Override
    public void deleteLessonsByIds(List<String> lessonIds) {
        List<Lesson> lessons = lessonRepository.findAllById(lessonIds);

        if (lessons.size() != lessonIds.size()) {
            throw new CustomException("Một số bài học không tìm thấy", HttpStatus.NOT_FOUND);
        }

        for (Lesson lesson : lessons) {
            Topic topic = lesson.getTopic();

            if (topic != null) {
                topic.getLessons().removeIf(existingLesson -> lessonIds.contains(existingLesson.getLessonId()));
                topicRepository.save(topic);
            }
            lesson.setDeletedAt(now);
        }


        lessonRepository.saveAll(lessons);
    }

    @Override
    public LessonResponse getLessonById(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new CustomException("Không tìm thấy bài học với ID: " + lessonId, HttpStatus.NOT_FOUND));

        lesson.setViewCount(lesson.getViewCount() + 1);
        lessonRepository.save(lesson);

        ZonedDateTime vietnamTime = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate today = vietnamTime.toLocalDate();
        Optional<LessonRecord> existingView = lessonRecordRepository.findByLessonIdAndCreatedAt(lessonId, today);

        if (existingView.isPresent()) {
            LessonRecord view = existingView.get();
            view.setViewCount(view.getViewCount() + 1);
            view.setUpdatedAt(now);
            lessonRecordRepository.save(view);
        } else {
            LessonRecord newView = new LessonRecord();
            newView.setLessonId(lessonId);
            newView.setGradeName(lesson.getTopic().getGrade().getGradeName());
            newView.setViewCount(1);
            newView.setCreatedAt(today);
            lessonRecordRepository.save(newView);
        }

        if (lesson.getLessonNumber() == 1 || isAuthenticated()) {
            return mapData.mapOne(lesson, LessonResponse.class);
        } else {
            throw new CustomException("Vui lòng đăng nhập để tiếp tục xem bài học.", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails;
    }
}