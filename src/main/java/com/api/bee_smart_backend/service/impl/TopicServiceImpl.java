package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.TopicLessonResponse;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.GradeRepository;
import com.api.bee_smart_backend.repository.QuizRepository;
import com.api.bee_smart_backend.repository.TopicRepository;
import com.api.bee_smart_backend.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    @Autowired
    private final GradeRepository gradeRepository;
    @Autowired
    private final TopicRepository topicRepository;
    @Autowired
    private final QuizRepository quizRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public Map<String, Object> getTopicsByGradeAndSemester(String grade, String semester, String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "topicNumber"));
        Grade existingGrade = gradeRepository.findByGradeNameAndDeletedAtIsNull(grade)
                .orElseThrow(() -> new CustomException("Lớp không tồn tại", HttpStatus.NOT_FOUND));

        // Step 1: Get all topics for the given grade and semester
        Page<Topic> topicPage = topicRepository.findByGrade_GradeIdAndSemesterAndDeletedAtIsNull(existingGrade.getGradeId(), semester, pageable);

        String chapter = switch (semester) {
            case "Học kì 1" -> "I";
            case "Học kì 2" -> "II";
            default -> "Unknown"; // Default value if semester does not match
        };

        // Step 2: Filter topics by search term
        List<Topic> filteredTopics = topicPage.getContent().stream()
                .filter(topic -> search == null || topic.getTopicName().toLowerCase().contains(search.toLowerCase()))
                .toList();

        // Step 3: If no topics match, search lessons
        if (filteredTopics.isEmpty() && search != null && !search.isBlank()) {
            filteredTopics = topicPage.getContent().stream()
                    .filter(topic -> topic.getLessons().stream()
                            .anyMatch(lesson -> lesson.getLessonName().toLowerCase().contains(search.toLowerCase()))
                    )
                    .toList();
        }

        // Step 4: If no topics and lessons match, search quizzes
        if (filteredTopics.isEmpty() && search != null && !search.isBlank()) {
            filteredTopics = topicPage.getContent().stream()
                    .filter(topic -> quizRepository.findByTopicInAndLessonIsNullAndDeletedAtIsNull(List.of(topic), pageable)
                            .getContent().stream()
                            .anyMatch(quiz -> quiz.getTitle().toLowerCase().contains(search.toLowerCase()))
                    )
                    .toList();
        }

        // Step 5: Build response for the filtered topics
        List<TopicLessonResponse> topics = filteredTopics.stream()
                .map(topic -> {
                    // Filter lessons by search
                    List<LessonResponse> lessons = topic.getLessons().stream()
                            .filter(lesson -> search == null || lesson.getLessonName().toLowerCase().contains(search.toLowerCase()))
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
                                        .lessonNumber(lesson.getLessonNumber())
                                        .description(lesson.getDescription())
                                        .content(lesson.getContent())
                                        .viewCount(lesson.getViewCount())
                                        .build();
                            })
                            .toList();

                    // Filter quizzes by search
                    List<QuizResponse> quizzes = quizRepository.findByTopicInAndLessonIsNullAndDeletedAtIsNull(List.of(topic), pageable)
                            .getContent().stream()
                            .filter(quiz -> search == null || quiz.getTitle().toLowerCase().contains(search.toLowerCase()))
                            .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                            .toList();

                    return TopicLessonResponse.builder()
                            .topicId(topic.getTopicId())
                            .topicName(topic.getTopicName())
                            .topicNumber(topic.getTopicNumber())
                            .lessons(lessons)
                            .quizzes(quizzes)
                            .build();
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", topics.size());
        response.put("totalPages", (int) Math.ceil((double) topics.size() / pageSize));
        response.put("currentPage", pageNumber);
        response.put("topics", topics);

        return response;
    }

    @Override
    public TopicResponse createTopicByGradeId(String gradeId, TopicRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy khối với ID: " + gradeId, HttpStatus.NOT_FOUND));

        Topic topic = Topic.builder()
                .topicName(request.getTopicName())
                .topicNumber(request.getTopicNumber())
                .grade(grade)
                .semester(request.getSemester())
                .createdAt(now)
                .build();

        Topic savedTopic = topicRepository.save(topic);
        grade.getTopics().add(savedTopic);
        gradeRepository.save(grade);

        return mapData.mapOne(savedTopic, TopicResponse.class);
    }

    @Override
    public TopicResponse updateTopicById(String topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        topic.setTopicName(request.getTopicName());
        topic.setTopicNumber(request.getTopicNumber());
        topic.setSemester(request.getSemester());
        topic.setUpdatedAt(now);

        Topic updatedTopic = topicRepository.save(topic);

        return mapData.mapOne(updatedTopic, TopicResponse.class);
    }

    @Override
    public TopicResponse getTopicById(String topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chủ đề với ID: " + topicId, HttpStatus.NOT_FOUND));

        return mapData.mapOne(topic, TopicResponse.class);
    }

    @Override
    public void deleteTopicById(String topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException("Chủ đề không tồn tại", HttpStatus.NOT_FOUND));

        if (!topic.getLessons().isEmpty()) {
            throw new CustomException("Không thể xóa chủ đề vì có bài học liên kết", HttpStatus.BAD_REQUEST);
        }

        Grade grade = topic.getGrade();

        if (grade != null) {
            grade.getTopics().removeIf(existingTopic -> existingTopic.getTopicId().equals(topicId));
            gradeRepository.save(grade);
        }
        topic.setDeletedAt(now);
        topicRepository.save(topic);
    }

    @Override
    public void deleteTopicsByIds(List<String> topicIds) {
        List<Topic> topics = topicRepository.findAllById(topicIds);

        if (topics.size() != topicIds.size()) {
            throw new CustomException("Một số chủ đề không tìm thấy", HttpStatus.NOT_FOUND);
        }

        for (Topic topic : topics) {
            if (!topic.getLessons().isEmpty()) {
                throw new CustomException(
                        "Không thể xóa chủ đề chứa bài học: " + topic.getTopicName(),
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        for (Topic topic : topics) {
            Grade grade = topic.getGrade();

            if (grade != null) {
                grade.getTopics().removeIf(existingTopic -> topicIds.contains(existingTopic.getTopicId()));
                gradeRepository.save(grade);
            }
            topic.setDeletedAt(now);
            topicRepository.save(topic);
        }
    }
}

