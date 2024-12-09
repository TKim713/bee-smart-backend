package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.view.LessonView;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final LessonViewRepository lessonViewRepository;

    private final MapData mapData;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public StatisticResponse getAggregatedStatisticByUserAndDateRange(String jwtToken, String startDate, String endDate) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        List<Statistic> statistics;
        LocalDate localStartDate = LocalDate.parse(startDate, formatter);
        LocalDate localEndDate = LocalDate.parse(endDate, formatter);

        if (user.getRole() == Role.PARENT) {
            Parent parent = (Parent) parentRepository.findByUserAndDeletedAtIsNull(user)
                    .orElseThrow(() -> new CustomException("Không tìm thấy tài khoản phụ huynh", HttpStatus.NOT_FOUND));
            List<Student> students = studentRepository.findByParent(parent);

            statistics = students.stream()
                    .map(Student::getUser)
                    .flatMap(studentUser -> statisticRepository.findByUserAndCreatedAtBetween(studentUser, localStartDate, localEndDate).stream())
                    .collect(Collectors.toList());
        } else {
            statistics = statisticRepository.findByUserAndCreatedAtBetween(user, localStartDate, localEndDate);
        }

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

        List<LessonView> views = lessonViewRepository.findAllByCreatedAtBetween(
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

        for (LessonView view : views) {
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
