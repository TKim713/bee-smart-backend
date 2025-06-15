package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.helper.response.TopicLessonResponse;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.model.BookType;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Subject;
import com.api.bee_smart_backend.model.Topic;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.stream.Collectors;

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
    @Autowired
    private final SubjectRepository subjectRepository;
    @Autowired
    private final BookTypeRepository bookTypeRepository;

    private final MapData mapData;
    private final Instant now = Instant.now();

    @Override
    public Map<String, Object> getTopicsAndLessonsBySubjectGradeAndSemester(String subject, String grade, String semester, String page, String size, String search, String bookType) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Grade existingGrade = gradeRepository.findByGradeNameAndDeletedAtIsNull(grade)
                .orElseThrow(() -> new CustomException("Lớp không tồn tại", HttpStatus.NOT_FOUND));

        Subject existingSubject = subjectRepository.findBySubjectNameAndDeletedAtIsNull(subject)
                .orElseThrow(() -> new CustomException("Môn học không tồn tại", HttpStatus.NOT_FOUND));

        // Lấy tất cả topics trước (không phân trang) để có thể search đầy đủ
        List<Topic> allTopics;
        if (bookType != null && !bookType.isBlank()) {
            BookType existingBookType = bookTypeRepository.findByBookNameAndDeletedAtIsNull(bookType)
                    .orElseThrow(() -> new CustomException("Loại sách không tồn tại", HttpStatus.NOT_FOUND));
            Page<Topic> allTopicsPage = topicRepository.findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndBookType_BookIdAndDeletedAtIsNull(
                    existingSubject.getSubjectId(), existingGrade.getGradeId(), semester, existingBookType.getBookId(), Pageable.unpaged());
            allTopics = allTopicsPage.getContent();
        } else {
            Page<Topic> allTopicsPage = topicRepository.findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndDeletedAtIsNull(
                    existingSubject.getSubjectId(), existingGrade.getGradeId(), semester, Pageable.unpaged());
            allTopics = allTopicsPage.getContent();
        }

        String chapter = switch (semester) {
            case "Học kì 1" -> "I";
            case "Học kì 2" -> "II";
            default -> "Unknown";
        };

        // Lọc topics dựa trên search
        List<Topic> filteredTopics;
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            filteredTopics = allTopics.stream()
                    .filter(topic -> {
                        // Tìm kiếm trong tên topic
                        boolean topicNameMatch = topic.getTopicName().toLowerCase().contains(searchLower);

                        // Tìm kiếm trong tên lessons
                        boolean lessonNameMatch = topic.getLessons().stream()
                                .anyMatch(lesson -> lesson.getLessonName().toLowerCase().contains(searchLower));

                        // Tìm kiếm trong quiz titles
                        boolean quizTitleMatch = quizRepository.findByTopicInAndLessonIsNullAndDeletedAtIsNull(List.of(topic), Pageable.unpaged())
                                .getContent().stream()
                                .anyMatch(quiz -> quiz.getTitle().toLowerCase().contains(searchLower));

                        return topicNameMatch || lessonNameMatch || quizTitleMatch;
                    })
                    .sorted((t1, t2) -> Integer.compare(t1.getTopicNumber(), t2.getTopicNumber()))
                    .collect(Collectors.toList());
        } else {
            filteredTopics = allTopics.stream()
                    .sorted((t1, t2) -> Integer.compare(t1.getTopicNumber(), t2.getTopicNumber()))
                    .collect(Collectors.toList());
        }

        // Tính toán phân trang cho kết quả đã lọc
        long totalItems = filteredTopics.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // Lấy topics cho trang hiện tại
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredTopics.size());
        List<Topic> pagedTopics = (fromIndex < filteredTopics.size()) ?
                filteredTopics.subList(fromIndex, toIndex) : List.of();

        // Build response với search highlighting logic
        List<TopicLessonResponse> topics = pagedTopics.stream()
                .map(topic -> {
                    // Lọc lessons dựa trên search
                    List<LessonResponse> lessons;
                    if (search != null && !search.isBlank()) {
                        String searchLower = search.toLowerCase();
                        lessons = topic.getLessons().stream()
                                .filter(lesson -> lesson.getLessonName().toLowerCase().contains(searchLower) ||
                                        topic.getTopicName().toLowerCase().contains(searchLower)) // Hiển thị tất cả lessons nếu topic match
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
                                .collect(Collectors.toList());
                    } else {
                        // Hiển thị tất cả lessons khi không có search
                        lessons = topic.getLessons().stream()
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
                                .collect(Collectors.toList());
                    }

                    // Lọc quizzes dựa trên search
                    List<QuizResponse> quizzes;
                    if (search != null && !search.isBlank()) {
                        String searchLower = search.toLowerCase();
                        quizzes = quizRepository.findByTopicInAndLessonIsNullAndDeletedAtIsNull(List.of(topic), Pageable.unpaged())
                                .getContent().stream()
                                .filter(quiz -> quiz.getTitle().toLowerCase().contains(searchLower) ||
                                        topic.getTopicName().toLowerCase().contains(searchLower)) // Hiển thị tất cả quizzes nếu topic match
                                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                                .collect(Collectors.toList());
                    } else {
                        // Hiển thị tất cả quizzes khi không có search
                        quizzes = quizRepository.findByTopicInAndLessonIsNullAndDeletedAtIsNull(List.of(topic), Pageable.unpaged())
                                .getContent().stream()
                                .map(quiz -> mapData.mapOne(quiz, QuizResponse.class))
                                .collect(Collectors.toList());
                    }

                    return TopicLessonResponse.builder()
                            .topicId(topic.getTopicId())
                            .topicName(topic.getTopicName())
                            .topicNumber(topic.getTopicNumber())
                            .lessons(lessons)
                            .quizzes(quizzes)
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        response.put("currentPage", pageNumber);
        response.put("topics", topics);

        return response;
    }

    @Override
    public TopicResponse createTopicByGradeIdSubjectIdAndBookTypeId(String gradeId, String subjectId, String bookTypeId, TopicRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy khối với ID: " + gradeId, HttpStatus.NOT_FOUND));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new CustomException("Không tìm thấy môn học với ID: " + subjectId, HttpStatus.NOT_FOUND));

        BookType bookType = bookTypeRepository.findById(bookTypeId)
                .orElseThrow(() -> new CustomException("Không tìm thấy loại sách với ID: " + bookTypeId, HttpStatus.NOT_FOUND));

        Topic topic = Topic.builder()
                .topicName(request.getTopicName())
                .topicNumber(request.getTopicNumber())
                .grade(grade)
                .subject(subject)
                .bookType(bookType) // Thêm bookType vào Topic
                .semester(request.getSemester())
                .createdAt(Instant.now())
                .build();

        Topic savedTopic = topicRepository.save(topic);
        subject.getTopics().add(savedTopic);
        subjectRepository.save(subject);

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

        Subject subject = topic.getSubject();

        if (subject != null) {
            subject.getTopics().removeIf(existingTopic -> existingTopic.getTopicId().equals(topicId));
            subjectRepository.save(subject);
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
            Subject subject = topic.getSubject();

            if (subject != null) {
                subject.getTopics().removeIf(existingTopic -> topicIds.contains(existingTopic.getTopicId()));
                subjectRepository.save(subject);
            }
            topic.setDeletedAt(now);
            topicRepository.save(topic);
        }
    }

    @Override
    public Map<String, Object> getTopicsBySubjectGradeAndSemester(String subject, String grade, String semester, String page, String size, String search, String bookType) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Grade existingGrade = gradeRepository.findByGradeNameAndDeletedAtIsNull(grade)
                .orElseThrow(() -> new CustomException("Lớp không tồn tại", HttpStatus.NOT_FOUND));

        Subject existingSubject = subjectRepository.findBySubjectNameAndDeletedAtIsNull(subject)
                .orElseThrow(() -> new CustomException("Môn học không tồn tại", HttpStatus.NOT_FOUND));

        // Lấy tất cả topics trước để có thể search đầy đủ
        List<Topic> allTopics;
        if (bookType != null && !bookType.isBlank()) {
            BookType existingBookType = bookTypeRepository.findByBookNameAndDeletedAtIsNull(bookType)
                    .orElseThrow(() -> new CustomException("Loại sách không tồn tại", HttpStatus.NOT_FOUND));
            Page<Topic> allTopicsPage = topicRepository.findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndBookType_BookIdAndDeletedAtIsNull(
                    existingSubject.getSubjectId(), existingGrade.getGradeId(), semester, existingBookType.getBookId(), Pageable.unpaged());
            allTopics = allTopicsPage.getContent();
        } else {
            Page<Topic> allTopicsPage = topicRepository.findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndDeletedAtIsNull(
                    existingSubject.getSubjectId(), existingGrade.getGradeId(), semester, Pageable.unpaged());
            allTopics = allTopicsPage.getContent();
        }

        // Lọc topics dựa trên search
        List<Topic> filteredTopics;
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            filteredTopics = allTopics.stream()
                    .filter(topic -> topic.getTopicName().toLowerCase().contains(searchLower))
                    .sorted((t1, t2) -> Integer.compare(t1.getTopicNumber(), t2.getTopicNumber()))
                    .collect(Collectors.toList());
        } else {
            filteredTopics = allTopics.stream()
                    .sorted((t1, t2) -> Integer.compare(t1.getTopicNumber(), t2.getTopicNumber()))
                    .collect(Collectors.toList());
        }

        // Tính toán phân trang cho kết quả đã lọc
        long totalItems = filteredTopics.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        // Lấy topics cho trang hiện tại
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredTopics.size());
        List<Topic> pagedTopics = (fromIndex < filteredTopics.size()) ?
                filteredTopics.subList(fromIndex, toIndex) : List.of();
        List<TopicResponse> topicResponses = pagedTopics.stream()
                .map(topic -> TopicResponse.builder()
                        .topicId(topic.getTopicId())
                        .topicName(topic.getTopicName())
                        .topicNumber(topic.getTopicNumber())
                        .gradeName(topic.getGrade().getGradeName())
                        .subjectName(topic.getSubject().getSubjectName())
                        .semester(topic.getSemester())
                        .bookName(topic.getBookType() != null ? topic.getBookType().getBookName() : null)
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        response.put("currentPage", pageNumber);
        response.put("topics", topicResponses);

        return response;
    }
}

