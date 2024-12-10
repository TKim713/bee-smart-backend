package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.record.LessonRecord;
import com.api.bee_smart_backend.model.record.QuizRecord;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {
    @Autowired
    private final ParentRepository parentRepository;
    @Autowired
    private final StudentRepository studentRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final StatisticRepository statisticRepository;
    @Autowired
    private final LessonRecordRepository lessonRecordRepository;
    @Autowired
    private final QuizRecordRepository quizRecordRepository;

    private final MapData mapData;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public StatisticResponse getAggregatedStatisticByUserAndDateRange(String userId, String startDate, String endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        List<Statistic> statistics;
        LocalDate localStartDate = LocalDate.parse(startDate, formatter);
        LocalDate localEndDate = LocalDate.parse(endDate, formatter);

        statistics = statisticRepository.findByUserAndCreatedAtBetween(user, localStartDate, localEndDate);

        StatisticResponse aggregatedResponse = new StatisticResponse();
        aggregatedResponse.setNumberOfQuestionsAnswered(
                statistics.stream().mapToInt(Statistic::getNumberOfQuestionsAnswered).sum());
        aggregatedResponse.setTimeSpentLearning(
                statistics.stream().mapToLong(Statistic::getTimeSpentLearning).sum());
        aggregatedResponse.setNumberOfQuizzesDone(
                statistics.stream().mapToInt(Statistic::getNumberOfQuizzesDone).sum());
        aggregatedResponse.setTimeSpentDoingQuizzes(
                statistics.stream().mapToLong(Statistic::getTimeSpentDoingQuizzes).sum());

        aggregatedResponse.setStartDate(localStartDate);
        aggregatedResponse.setEndDate(localEndDate);

        return aggregatedResponse;
    }

    @Override
    public Map<String, Map<String, Integer>> getViewLessonByMonth(String date) {
        YearMonth currentYearMonth = YearMonth.now();
        LocalDate startDate = getLocalDate(date, currentYearMonth);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDate currentDate = LocalDate.now();

        if (currentDate.isBefore(endDate)) {
            endDate = currentDate;
        }

        List<LessonRecord> views = lessonRecordRepository.findAllByCreatedAtBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        Map<String, Map<String, Integer>> chartData = new LinkedHashMap<>();
        List<String> grades = List.of("Lớp 1", "Lớp 2", "Lớp 3", "Lớp 4", "Lớp 5");

        for (LocalDate dateIter = startDate; !dateIter.isAfter(endDate); dateIter = dateIter.plusDays(1)) {
            String dateStr = dateIter.format(DateTimeFormatter.ofPattern("dd-MM"));
            chartData.putIfAbsent(dateStr, new LinkedHashMap<>());

            for (String grade : grades) {
                chartData.get(dateStr).putIfAbsent(grade, 0);
            }
        }

        for (LessonRecord view : views) {
            String dateStr = view.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM")); // Định dạng "ngày-tháng"
            String gradeName = view.getGradeName();

            if (gradeName == null) {
                continue;
            }

            chartData.putIfAbsent(dateStr, new LinkedHashMap<>());

            Map<String, Integer> gradeViews = chartData.get(dateStr);

            int currentViews = gradeViews.getOrDefault(gradeName, 0);
            gradeViews.put(gradeName, currentViews + view.getViewCount());
        }

        return chartData;
    }

    @Override
    public Map<String, Object> getListQuizRecord(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<QuizRecord> quizRecordPage;

        if (search == null || search.isBlank()) {
            quizRecordPage = quizRecordRepository.findAll(pageable);
        } else {
            quizRecordPage = quizRecordRepository.findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(search, search, pageable);
        }

        List<QuizRecordResponse> quizRecordResponses = quizRecordPage.getContent().stream()
                .map(quizRecord -> QuizRecordResponse.builder()
                        .recordId(quizRecord.getRecordId())
                        .username(quizRecord.getUser().getUsername())
                        .quizName(quizRecord.getQuiz().getTitle())
                        .totalQuestions(quizRecord.getTotalQuestions())
                        .correctAnswers(quizRecord.getCorrectAnswers())
                        .points(quizRecord.getPoints())
                        .timeSpent(quizRecord.getTimeSpent())
                        .createdAt(quizRecord.getCreatedAt())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalItems", quizRecordPage.getTotalElements());
        response.put("totalPages", quizRecordPage.getTotalPages());
        response.put("currentPage", quizRecordPage.getNumber());
        response.put("quizRecords", quizRecordResponses);

        return response;
    }

    @Override
    public Map<String, Double> getQuizStatistics() {
        List<QuizRecord> quizRecords = quizRecordRepository.findAll();

        Map<String, Integer> gradeCount = new HashMap<>();
        for (QuizRecord record : quizRecords) {
            String gradeName = record.getGradeName();
            if (gradeName == null) continue;

            gradeCount.put(gradeName, gradeCount.getOrDefault(gradeName, 0) + 1);
        }

        int totalQuizzes = gradeCount.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Double> chartData = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : gradeCount.entrySet()) {
            String gradeName = entry.getKey();
            int count = entry.getValue();
            double percentage = (double) count / totalQuizzes * 100;

            chartData.put(gradeName, Math.round(percentage * 100.0) / 100.0);
        }

        return chartData;
    }

    @Override
    public Map<String, Map<String, Integer>> getQuizByMonth(String date) {
        YearMonth currentYearMonth = YearMonth.now();
        LocalDate startDate = getLocalDate(date, currentYearMonth);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDate currentDate = LocalDate.now();
        if (currentDate.isBefore(endDate)) {
            endDate = currentDate;
        }

        List<QuizRecord> quizRecords = quizRecordRepository.findAllBySubmitDateBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        Map<String, Map<String, Integer>> chartData = new LinkedHashMap<>();
        List<String> grades = List.of("Lớp 1", "Lớp 2", "Lớp 3", "Lớp 4", "Lớp 5");

        for (LocalDate dateIter = startDate; !dateIter.isAfter(endDate); dateIter = dateIter.plusDays(1)) {
            String dateStr = dateIter.format(DateTimeFormatter.ofPattern("dd-MM"));
            chartData.putIfAbsent(dateStr, new LinkedHashMap<>());

            for (String grade : grades) {
                chartData.get(dateStr).putIfAbsent(grade, 0);
            }
        }

        for (QuizRecord record : quizRecords) {
            String dateStr = record.getSubmitDate().format(DateTimeFormatter.ofPattern("dd-MM")); // Lấy ngày dưới dạng dd-MM
            String gradeName = record.getGradeName();

            if (gradeName == null) {
                continue;
            }

            chartData.putIfAbsent(dateStr, new LinkedHashMap<>());

            Map<String, Integer> gradeQuizCount = chartData.get(dateStr);
            int currentCount = gradeQuizCount.getOrDefault(gradeName, 0);
            gradeQuizCount.put(gradeName, currentCount + 1); // Tăng số lượng bài làm của lớp đó
        }

        return chartData;
    }

    private static LocalDate getLocalDate(String date, YearMonth currentYearMonth) {
        int queryYear;
        int queryMonth;

        if (date != null && !date.isEmpty()) {
            // Chia chuỗi date thành tháng và năm (MM-yyyy)
            String[] parts = date.split("-");
            if (parts.length == 2) {
                try {
                    queryMonth = Integer.parseInt(parts[0]); // Month part
                    queryYear = Integer.parseInt(parts[1]); // Year part
                } catch (NumberFormatException e) {
                    // Nếu không thể phân tích tháng/năm, sử dụng năm tháng hiện tại
                    queryYear = currentYearMonth.getYear();
                    queryMonth = currentYearMonth.getMonthValue();
                }
            } else {
                // Nếu định dạng không đúng, sử dụng tháng và năm hiện tại
                queryYear = currentYearMonth.getYear();
                queryMonth = currentYearMonth.getMonthValue();
            }
        } else {
            // Nếu không có tham số date, sử dụng tháng và năm hiện tại
            queryYear = currentYearMonth.getYear();
            queryMonth = currentYearMonth.getMonthValue();
        }

        // Xác định ngày bắt đầu và ngày kết thúc của tháng
        return LocalDate.of(queryYear, queryMonth, 1);
    }
}