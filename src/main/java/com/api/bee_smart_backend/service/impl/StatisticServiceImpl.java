package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.dto.PlayerScore;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final LessonRecordRepository lessonRecordRepository;
    @Autowired
    private final QuizRecordRepository quizRecordRepository;
    @Autowired
    private final TopicRepository topicRepository;
    @Autowired
    private final SubjectRepository subjectRepository;
    @Autowired
    private final BattleRepository battleRepository;
    @Autowired
    private final GradeRepository gradeRepository;

    private final MapData mapData;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public StatisticResponse getAggregatedStatisticByUserAndDateRange(String userId, String startDate, String endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        LocalDate localStartDate = LocalDate.parse(startDate, formatter);
        LocalDate localEndDate = LocalDate.parse(endDate, formatter);

        LocalDateTime startOfDay = localStartDate.atStartOfDay();
        LocalDateTime endOfDay = localEndDate.atTime(LocalTime.MAX);

        List<QuizRecord> quizRecords = quizRecordRepository.findByUserAndSubmitDateBetween(user, startOfDay, endOfDay);

        StatisticResponse aggregatedResponse = new StatisticResponse();
        aggregatedResponse.setNumberOfQuestionsAnswered(
                quizRecords.stream().mapToInt(QuizRecord::getTotalQuestions).sum());
        aggregatedResponse.setTimeSpentLearning(
                quizRecords.stream().mapToLong(QuizRecord::getTimeSpent).sum());
        aggregatedResponse.setNumberOfQuizzesDone(quizRecords.size());
        aggregatedResponse.setTimeSpentDoingQuizzes(
                quizRecords.stream().mapToLong(QuizRecord::getTimeSpent).sum());

        aggregatedResponse.setStartDate(localStartDate);
        aggregatedResponse.setEndDate(localEndDate);

        return aggregatedResponse;
    }

    @Override
    public Map<String, Map<String, Integer>> getViewLessonByMonth(String date, String subjectName) {
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

        // Filter by subject if provided
        if (subjectName != null && !subjectName.isBlank()) {
            List<String> lessonIdsForSubject = getLessonIdsBySubject(subjectName);
            views = views.stream()
                    .filter(view -> lessonIdsForSubject.contains(view.getLessonId()))
                    .toList();
        }

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
            String dateStr = view.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM"));
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
    public Map<String, Object> getListQuizRecord(String page, String size, String search, String subjectName) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<QuizRecord> quizRecordPage;

        if (search == null || search.isBlank()) {
            quizRecordPage = quizRecordRepository.findAll(pageable);
        } else {
            quizRecordPage = quizRecordRepository.findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(search, search, pageable);
        }

        List<QuizRecord> filteredRecords = quizRecordPage.getContent();

        // Filter by subject if provided
        if (subjectName != null && !subjectName.isBlank()) {
            filteredRecords = filteredRecords.stream()
                    .filter(record -> {
                        Quiz quiz = record.getQuiz();
                        if (quiz == null) return false;

                        // Check if quiz has a lesson with the subject
                        if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                            Subject subject = quiz.getLesson().getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        // Check if quiz has a topic with the subject
                        if (quiz.getTopic() != null) {
                            Subject subject = quiz.getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        return false;
                    })
                    .toList();
        }

        List<QuizRecordResponse> quizRecordResponses = filteredRecords.stream()
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
        response.put("totalItems", (long) filteredRecords.size());
        response.put("totalPages", (int) Math.ceil((double) filteredRecords.size() / pageSize));
        response.put("currentPage", pageNumber);
        response.put("quizRecords", quizRecordResponses);

        return response;
    }

    @Override
    public Map<String, Object> getListQuizRecordByUser(String userId, String page, String size, String search, String subjectName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<QuizRecord> quizRecordPage;

        if (search == null || search.isBlank()) {
            quizRecordPage = quizRecordRepository.findByUser(user, pageable);
        } else {
            quizRecordPage = quizRecordRepository.findByUserAndQuizTitleContainingIgnoreCase(user, search, pageable);
        }

        List<QuizRecord> filteredRecords = quizRecordPage.getContent();

        // Filter by subject if provided
        if (subjectName != null && !subjectName.isBlank()) {
            filteredRecords = filteredRecords.stream()
                    .filter(record -> {
                        Quiz quiz = record.getQuiz();
                        if (quiz == null) return false;

                        // Check if quiz has a lesson with the subject
                        if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                            Subject subject = quiz.getLesson().getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        // Check if quiz has a topic with the subject
                        if (quiz.getTopic() != null) {
                            Subject subject = quiz.getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        return false;
                    })
                    .collect(Collectors.toList());
        }

        List<QuizRecordResponse> quizRecordResponses = filteredRecords.stream()
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
        response.put("totalItems", (long) filteredRecords.size());
        response.put("totalPages", (int) Math.ceil((double) filteredRecords.size() / pageSize));
        response.put("currentPage", pageNumber);
        response.put("quizRecords", quizRecordResponses);

        return response;
    }

    @Override
    public Map<String, Double> getQuizStatistics(String subjectName) {
        List<Grade> grades = gradeRepository.findAll();
        List<QuizRecord> quizRecords = quizRecordRepository.findAll();

        if (subjectName != null && !subjectName.isBlank()) {
            quizRecords = quizRecords.stream()
                    .filter(record -> {
                        Quiz quiz = record.getQuiz();
                        if (quiz == null) return false;

                        if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                            Subject subject = quiz.getLesson().getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        if (quiz.getTopic() != null) {
                            Subject subject = quiz.getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        return false;
                    })
                    .toList();
        }

        Map<String, Integer> gradeCount = new HashMap<>();
        // Initialize all grades with 0 count
        for (Grade grade : grades) {
            gradeCount.put(grade.getGradeName(), 0);
        }

        // Count quiz records per grade
        for (QuizRecord record : quizRecords) {
            String gradeName = record.getGradeName();
            if (gradeName == null) continue;

            gradeCount.put(gradeName, gradeCount.getOrDefault(gradeName, 0) + 1);
        }

        int totalQuizzes = gradeCount.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Double> chartData = new LinkedHashMap<>();
        for (Grade grade : grades) {
            String gradeName = grade.getGradeName();
            int count = gradeCount.getOrDefault(gradeName, 0);
            double percentage = totalQuizzes > 0 ? (double) count / totalQuizzes * 100 : 0;
            chartData.put(gradeName, Math.round(percentage * 100.0) / 100.0);
        }

        return chartData;
    }

    @Override
    public Map<String, Map<String, Integer>> getQuizByMonth(String date, String subjectName) {
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

        // Filter by subject if provided
        if (subjectName != null && !subjectName.isBlank()) {
            quizRecords = quizRecords.stream()
                    .filter(record -> {
                        Quiz quiz = record.getQuiz();
                        if (quiz == null) return false;

                        // Check if quiz has a lesson with the subject
                        if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                            Subject subject = quiz.getLesson().getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        // Check if quiz has a topic with the subject
                        if (quiz.getTopic() != null) {
                            Subject subject = quiz.getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        return false;
                    })
                    .toList();
        }

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
            String dateStr = record.getSubmitDate().format(DateTimeFormatter.ofPattern("dd-MM"));
            String gradeName = record.getGradeName();

            if (gradeName == null) {
                continue;
            }

            chartData.putIfAbsent(dateStr, new LinkedHashMap<>());

            Map<String, Integer> gradeQuizCount = chartData.get(dateStr);
            int currentCount = gradeQuizCount.getOrDefault(gradeName, 0);
            gradeQuizCount.put(gradeName, currentCount + 1);
        }

        return chartData;
    }

    @Override
    public Map<String, Map<String, Double>> getQuizAverageByMonth(String date, String subjectName) {
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

        // Filter by subject if provided
        if (subjectName != null && !subjectName.isBlank()) {
            quizRecords = quizRecords.stream()
                    .filter(record -> {
                        Quiz quiz = record.getQuiz();
                        if (quiz == null) return false;

                        // Check if quiz has a lesson with the subject
                        if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                            Subject subject = quiz.getLesson().getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        // Check if quiz has a topic with the subject
                        if (quiz.getTopic() != null) {
                            Subject subject = quiz.getTopic().getSubject();
                            return subject != null && subjectName.equals(subject.getSubjectName());
                        }

                        return false;
                    })
                    .toList();
        }

        Map<String, Map<String, Integer>> totalQuizCount = new LinkedHashMap<>();
        Map<String, Map<String, Double>> totalPoints = new LinkedHashMap<>();
        Map<String, Map<String, Double>> averageScores = new LinkedHashMap<>();

        List<String> grades = List.of("Lớp 1", "Lớp 2", "Lớp 3", "Lớp 4", "Lớp 5");

        for (LocalDate dateIter = startDate; !dateIter.isAfter(endDate); dateIter = dateIter.plusDays(1)) {
            String dateStr = dateIter.format(DateTimeFormatter.ofPattern("dd-MM"));
            totalPoints.putIfAbsent(dateStr, new LinkedHashMap<>());
            totalQuizCount.putIfAbsent(dateStr, new LinkedHashMap<>());
            averageScores.putIfAbsent(dateStr, new LinkedHashMap<>());

            for (String grade : grades) {
                totalPoints.get(dateStr).put(grade, 0.0);
                totalQuizCount.get(dateStr).put(grade, 0);
                averageScores.get(dateStr).put(grade, 0.0);
            }
        }

        for (QuizRecord record : quizRecords) {
            String dateStr = record.getSubmitDate().format(DateTimeFormatter.ofPattern("dd-MM"));
            String gradeName = record.getGradeName();

            if (gradeName == null) {
                continue;
            }

            totalPoints.putIfAbsent(dateStr, new LinkedHashMap<>());
            totalQuizCount.putIfAbsent(dateStr, new LinkedHashMap<>());

            double currentTotalPoints = totalPoints.get(dateStr).getOrDefault(gradeName, 0.0);
            int currentTotalQuizzes = totalQuizCount.get(dateStr).getOrDefault(gradeName, 0);

            totalPoints.get(dateStr).put(gradeName, currentTotalPoints + record.getPoints());
            totalQuizCount.get(dateStr).put(gradeName, currentTotalQuizzes + 1);
        }

        for (String dateStr : totalPoints.keySet()) {
            Map<String, Double> dailyPoints = totalPoints.get(dateStr);
            Map<String, Integer> dailyQuizCount = totalQuizCount.get(dateStr);
            Map<String, Double> dailyAverageScores = averageScores.get(dateStr);

            for (String grade : grades) {
                double totalPointsForGrade = dailyPoints.getOrDefault(grade, 0.0);
                int totalQuizzesForGrade = dailyQuizCount.getOrDefault(grade, 0);

                if (totalQuizzesForGrade > 0) {
                    double averageScore = totalPointsForGrade / totalQuizzesForGrade;
                    dailyAverageScores.put(grade, Math.round(averageScore * 100.0) / 100.0);
                } else {
                    dailyAverageScores.put(grade, 0.0);
                }
            }
        }

        return averageScores;
    }

    @Override
    public Map<String, Map<String, Integer>> getQuizScoreStatisticsBySubject() {
        List<QuizRecord> quizRecords = quizRecordRepository.findAll();
        List<Subject> subjects = subjectRepository.findAll();

        List<String> scoreGroups = List.of(
                "0.0 - 3.4",
                "3.5 - 4.9",
                "5.0 - 6.4",
                "6.5 - 7.9",
                "8.0 - 10.0"
        );

        Map<String, Map<String, Integer>> chartData = new LinkedHashMap<>();

        // Initialize chart data for all subjects
        for (Subject subject : subjects) {
            String subjectName = subject.getSubjectName();
            chartData.put(subjectName, new LinkedHashMap<>());
            for (String scoreGroup : scoreGroups) {
                chartData.get(subjectName).put(scoreGroup, 0);
            }
        }

        // Process quiz records
        for (QuizRecord record : quizRecords) {
            Quiz quiz = record.getQuiz();
            if (quiz == null) continue;

            String subjectName = null;
            if (quiz.getLesson() != null && quiz.getLesson().getTopic() != null) {
                Subject subject = quiz.getLesson().getTopic().getSubject();
                if (subject != null) {
                    subjectName = subject.getSubjectName();
                }
            } else if (quiz.getTopic() != null) {
                Subject subject = quiz.getTopic().getSubject();
                if (subject != null) {
                    subjectName = subject.getSubjectName();
                }
            }

            if (subjectName == null) continue;

            double points = record.getPoints();
            String scoreGroup = getScoreGroup(points);

            Map<String, Integer> subjectData = chartData.get(subjectName);
            if (subjectData != null) {
                int currentCount = subjectData.getOrDefault(scoreGroup, 0);
                subjectData.put(scoreGroup, currentCount + 1);
            }
        }

        // Convert counts to percentages
        for (String subjectName : chartData.keySet()) {
            Map<String, Integer> scoreData = chartData.get(subjectName);
            int totalScores = scoreData.values().stream().mapToInt(Integer::intValue).sum();
            for (String scoreGroup : scoreGroups) {
                int count = scoreData.get(scoreGroup);
                double percentage = totalScores > 0 ? (double) count / totalScores * 100 : 0;
                scoreData.put(scoreGroup, (int) Math.round(percentage));
            }
        }

        return chartData;
    }

    private String getScoreGroup(double score) {
        if (score >= 0.0 && score <= 3.4) {
            return "0.0 - 3.4";
        } else if (score >= 3.5 && score <= 4.9) {
            return "3.5 - 4.9";
        } else if (score >= 5.0 && score <= 6.4) {
            return "5.0 - 6.4";
        } else if (score >= 6.5 && score <= 7.9) {
            return "6.5 - 7.9";
        } else if (score >= 8.0 && score <= 10.0) {
            return "8.0 - 10.0";
        } else {
            return "0.0 - 3.4"; // Handle edge cases
        }
    }

    private List<String> getLessonIdsBySubject(String subjectName) {
        Subject subject = subjectRepository.findBySubjectName(subjectName);
        if (subject == null) {
            return List.of();
        }

        List<Topic> topics = topicRepository.findBySubject(subject);

        return topics.stream()
                .flatMap(topic -> topic.getLessons().stream())
                .map(Lesson::getLessonId)
                .collect(Collectors.toList());
    }

    private static LocalDate getLocalDate(String date, YearMonth currentYearMonth) {
        int queryYear;
        int queryMonth;

        if (date != null && !date.isEmpty()) {
            String[] parts = date.split("-");
            if (parts.length == 2) {
                try {
                    queryMonth = Integer.parseInt(parts[0]);
                    queryYear = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    queryYear = currentYearMonth.getYear();
                    queryMonth = currentYearMonth.getMonthValue();
                }
            } else {
                queryYear = currentYearMonth.getYear();
                queryMonth = currentYearMonth.getMonthValue();
            }
        } else {
            queryYear = currentYearMonth.getYear();
            queryMonth = currentYearMonth.getMonthValue();
        }

        return LocalDate.of(queryYear, queryMonth, 1);
    }

    @Override
    public Map<String, Integer> getUsersJoinedBattleBySubject() {
        List<Subject> subjects = subjectRepository.findAll();
        List<Battle> battles = battleRepository.findAll();

        Map<String, Set<String>> subjectUserMap = new HashMap<>();
        // Initialize all subjects with empty user sets
        for (Subject subject : subjects) {
            subjectUserMap.put(subject.getSubjectName(), new HashSet<>());
        }

        // Aggregate users by subject
        for (Battle battle : battles) {
            String subjectId = battle.getSubjectId();
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject == null) continue;

            String subjectName = subject.getSubjectName();
            for (PlayerScore playerScore : battle.getPlayerScores()) {
                subjectUserMap.get(subjectName).add(playerScore.getUserId());
            }
        }

        Map<String, Integer> chartData = new LinkedHashMap<>();
        int totalUsers = subjectUserMap.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .mapToInt(v -> 1)
                .sum();

        for (Map.Entry<String, Set<String>> entry : subjectUserMap.entrySet()) {
            int userCount = entry.getValue().size();
            double percentage = totalUsers > 0 ? (double) userCount / totalUsers * 100 : 0;
            chartData.put(entry.getKey(), (int) Math.round(percentage));
        }

        return chartData;
    }

    @Override
    public Map<String, Map<String, Integer>> getBattleScoreDistributionBySubject() {
        List<Subject> subjects = subjectRepository.findAll();
        List<Battle> battles = battleRepository.findAll();

        List<String> scoreRanges = List.of("0-50", "51-70", "71-90", "91-100");
        Map<String, Map<String, Integer>> chartData = new LinkedHashMap<>();

        // Initialize chart data for all subjects
        for (Subject subject : subjects) {
            String subjectName = subject.getSubjectName();
            chartData.put(subjectName, new LinkedHashMap<>());
            for (String range : scoreRanges) {
                chartData.get(subjectName).put(range, 0);
            }
        }

        // Process battle records
        for (Battle battle : battles) {
            String subjectId = battle.getSubjectId();
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject == null) continue;

            String subjectName = subject.getSubjectName();
            for (PlayerScore playerScore : battle.getPlayerScores()) {
                int score = playerScore.getScore();
                String scoreRange = getBattleScoreRange(score);

                Map<String, Integer> subjectData = chartData.get(subjectName);
                int currentCount = subjectData.getOrDefault(scoreRange, 0);
                subjectData.put(scoreRange, currentCount + 1);
            }
        }

        // Convert counts to percentages
        for (String subjectName : chartData.keySet()) {
            Map<String, Integer> scoreData = chartData.get(subjectName);
            int totalScores = scoreData.values().stream().mapToInt(Integer::intValue).sum();
            for (String range : scoreRanges) {
                int count = scoreData.get(range);
                double percentage = totalScores > 0 ? (double) count / totalScores * 100 : 0;
                scoreData.put(range, (int) Math.round(percentage));
            }
        }

        return chartData;
    }

    @Override
    public Map<String, Double> getBattleAveragePointsByMonth(String date, String subjectName, String gradeName) {
        YearMonth currentYearMonth = YearMonth.now();
        LocalDate startDate = getLocalDate(date, currentYearMonth);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDate currentDate = LocalDate.now();
        if (currentDate.isBefore(endDate)) {
            endDate = currentDate;
        }

        List<Battle> battles = battleRepository.findAllByStartTimeBetween(startDate, endDate);

        if (subjectName != null && !subjectName.isBlank()) {
            battles = battles.stream()
                    .filter(battle -> {
                        Subject subject = subjectRepository.findById(battle.getSubjectId()).orElse(null);
                        return subject != null && subjectName.equals(subject.getSubjectName());
                    })
                    .toList();
        }

        if (gradeName != null && !gradeName.isBlank()) {
            battles = battles.stream()
                    .filter(battle -> {
                        Grade grade = gradeRepository.findById(battle.getGradeId()).orElse(null);
                        return grade != null && gradeName.equals(grade.getGradeName());
                    })
                    .toList();
        }

        Map<String, Double> totalPoints = new LinkedHashMap<>();
        Map<String, Integer> battleCount = new LinkedHashMap<>();
        Map<String, Double> chartData = new LinkedHashMap<>();

        for (LocalDate dateIter = startDate; !dateIter.isAfter(endDate); dateIter = dateIter.plusDays(1)) {
            String dateStr = dateIter.format(DateTimeFormatter.ofPattern("dd-MM"));
            totalPoints.put(dateStr, 0.0);
            battleCount.put(dateStr, 0);
            chartData.put(dateStr, 0.0);
        }

        for (Battle battle : battles) {
            LocalDate startTime = battle.getStartTime();
            if (startTime == null) continue;

            String dateStr = startTime.format(DateTimeFormatter.ofPattern("dd-MM"));
            for (PlayerScore playerScore : battle.getPlayerScores()) {
                double currentPoints = totalPoints.getOrDefault(dateStr, 0.0);
                int currentCount = battleCount.getOrDefault(dateStr, 0);
                totalPoints.put(dateStr, currentPoints + playerScore.getScore());
                battleCount.put(dateStr, currentCount + 1);
            }
        }

        for (String dateStr : totalPoints.keySet()) {
            double points = totalPoints.get(dateStr);
            int count = battleCount.get(dateStr);
            double average = count > 0 ? points / count : 0.0;
            chartData.put(dateStr, Math.round(average * 100.0) / 100.0);
        }

        return chartData;
    }

    private String getBattleScoreRange(int score) {
        if (score >= 0 && score <= 50) {
            return "0-50";
        } else if (score >= 51 && score <= 70) {
            return "51-70";
        } else if (score >= 71 && score <= 90) {
            return "71-90";
        } else if (score >= 91 && score <= 100) {
            return "91-100";
        } else {
            return "0-50"; // Handle edge cases
        }
    }
}
