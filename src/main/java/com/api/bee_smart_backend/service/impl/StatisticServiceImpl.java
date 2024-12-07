package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
            Parent parent = (Parent) parentRepository.findByUser(user)
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
}
