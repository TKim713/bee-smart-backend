package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizRecordResponse;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.dto.PlayerScore;
import com.api.bee_smart_backend.model.record.LessonRecord;
import com.api.bee_smart_backend.model.record.QuizRecord;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.impl.StatisticServiceImpl;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRecordRepository lessonRecordRepository;

    @Mock
    private QuizRecordRepository quizRecordRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private MapData mapData;

    @InjectMocks
    private StatisticServiceImpl statisticService;

    private User testUser;
    private Quiz testQuiz;
    private Subject testSubject;
    private Topic testTopic;
    private Lesson testLesson;
    private Grade testGrade;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("user1")
                .username("testUser")
                .build();

        testSubject = Subject.builder()
                .subjectId("sub1")
                .subjectName("Math")
                .build();

        testLesson = Lesson.builder()
                .lessonId("lesson1")
                .build(); // Initialize lesson first to avoid circular dependency

        testTopic = Topic.builder()
                .topicId("topic1")
                .subject(testSubject)
                .lessons(List.of(testLesson)) // Include testLesson to fix filtering issue
                .build();

        // Update testLesson to reference testTopic
        testLesson.setTopic(testTopic);

        testQuiz = Quiz.builder()
                .quizId("quiz1")
                .title("Test Quiz")
                .lesson(testLesson)
                .topic(testTopic)
                .build();

        testGrade = Grade.builder()
                .gradeId("grade1")
                .gradeName("Lớp 1")
                .build();
    }

    @Test
    void getViewLessonByMonth_Success() {
        // Arrange
        String date = "06-2025";
        String subjectName = "Math";

        LocalDate startDate = LocalDate.of(2025, 6, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 30);

        LessonRecord lessonRecord = LessonRecord.builder()
                .lessonViewId("view1")
                .lessonId("lesson1")
                .gradeName("Lớp 1")
                .viewCount(5)
                .createdAt(LocalDate.of(2025, 6, 1))
                .build();

        when(subjectRepository.findBySubjectName(subjectName)).thenReturn(testSubject);
        when(topicRepository.findBySubject(testSubject)).thenReturn(List.of(testTopic));
        when(lessonRecordRepository.findAllByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(lessonRecord));

        // Act
        Map<String, Map<String, Integer>> result = statisticService.getViewLessonByMonth(date, subjectName);

        // Assert
        assertNotNull(result, "Result map should not be null");
        assertTrue(result.containsKey("01-06"), "Result should contain date 01-06");
        assertEquals(5, result.get("01-06").get("Lớp 1"), "View count for Lớp 1 on 01-06 should be 5");
        assertEquals(0, result.get("01-06").get("Lớp 2"), "View count for Lớp 2 on 01-06 should be 0");

        verify(subjectRepository).findBySubjectName(subjectName);
        verify(topicRepository).findBySubject(testSubject);
        verify(lessonRecordRepository).findAllByCreatedAtBetween(any(), any());
    }

    // Other test methods remain unchanged for brevity
    // Include them as provided in the previous corrected version
    @Test
    void getAggregatedStatisticByUserAndDateRange_Success() {
        String userId = "user1";
        String startDate = "01-06-2025";
        String endDate = "30-06-2025";

        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 30, 23, 59, 59, 999999999);

        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .user(testUser)
                .quiz(testQuiz)
                .totalQuestions(10)
                .correctAnswers(8)
                .points(8.0)
                .timeSpent(300L)
                .submitDate(LocalDate.of(2025, 6, 15))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(quizRecordRepository.findByUserAndSubmitDateBetween(testUser, start, end))
                .thenReturn(List.of(quizRecord));

        StatisticResponse response = statisticService.getAggregatedStatisticByUserAndDateRange(userId, startDate, endDate);

        assertNotNull(response);
        assertEquals(10, response.getNumberOfQuestionsAnswered());
        assertEquals(300L, response.getTimeSpentLearning());
        assertEquals(1, response.getNumberOfQuizzesDone());
        assertEquals(300L, response.getTimeSpentDoingQuizzes());
        assertEquals(LocalDate.of(2025, 6, 1), response.getStartDate());
        assertEquals(LocalDate.of(2025, 6, 30), response.getEndDate());

        verify(userRepository).findById(userId);
        verify(quizRecordRepository).findByUserAndSubmitDateBetween(testUser, start, end);
    }

    @Test
    void getAggregatedStatisticByUserAndDateRange_UserNotFound_ThrowsException() {
        String userId = "user1";
        String startDate = "01-06-2025";
        String endDate = "30-06-2025";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () ->
                statisticService.getAggregatedStatisticByUserAndDateRange(userId, startDate, endDate));
        assertEquals("Không tìm thấy người dùng", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(userRepository).findById(userId);
    }

    @Test
    void getListQuizRecord_Success() {
        String page = "0";
        String size = "10";
        String search = "test";
        String subjectName = "Math";

        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .user(testUser)
                .quiz(testQuiz)
                .gradeName("Lớp 1")
                .totalQuestions(10)
                .correctAnswers(8)
                .points(8.0)
                .timeSpent(300L)
                .createdAt(Instant.now())
                .build();

        Page<QuizRecord> quizRecordPage = new PageImpl<>(List.of(quizRecord));
        when(quizRecordRepository.findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(eq(search), eq(search), any(Pageable.class)))
                .thenReturn(quizRecordPage);

        Map<String, Object> result = statisticService.getListQuizRecord(page, size, search, subjectName);

        assertNotNull(result);
        assertEquals(1L, result.get("totalItems"));
        assertEquals(1, result.get("totalPages"));
        assertEquals(0, result.get("currentPage"));
        List<QuizRecordResponse> quizRecords = (List<QuizRecordResponse>) result.get("quizRecords");
        assertEquals(1, quizRecords.size());
        assertEquals("testUser", quizRecords.get(0).getUsername());
        assertEquals("Test Quiz", quizRecords.get(0).getQuizName());

        verify(quizRecordRepository).findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(eq(search), eq(search), any(Pageable.class));
    }

    @Test
    void getQuizStatistics_Success() {
        String subjectName = "Math";

        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .quiz(testQuiz)
                .gradeName("Lớp 1")
                .points(8.0)
                .build();

        when(gradeRepository.findAll()).thenReturn(List.of(testGrade));
        when(quizRecordRepository.findAll()).thenReturn(List.of(quizRecord));

        Map<String, Double> result = statisticService.getQuizStatistics(subjectName);

        assertNotNull(result);
        assertEquals(100.0, result.get("Lớp 1"));

        verify(gradeRepository).findAll();
        verify(quizRecordRepository).findAll();
    }

    @Test
    void getQuizByMonth_Success() {
        String date = "06-2025";
        String subjectName = "Math";

        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .quiz(testQuiz)
                .gradeName("Lớp 1")
                .submitDate(LocalDate.of(2025, 6, 1))
                .build();

        when(quizRecordRepository.findAllBySubmitDateBetween(any(), any()))
                .thenReturn(List.of(quizRecord));

        Map<String, Map<String, Integer>> result = statisticService.getQuizByMonth(date, subjectName);

        assertNotNull(result);
        assertTrue(result.containsKey("01-06"));
        assertEquals(1, result.get("01-06").get("Lớp 1"));
        assertEquals(0, result.get("01-06").get("Lớp 2"));

        verify(quizRecordRepository).findAllBySubmitDateBetween(any(), any());
    }

    @Test
    void getQuizAverageByMonth_Success() {
        String date = "06-2025";
        String subjectName = "Math";

        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .quiz(testQuiz)
                .gradeName("Lớp 1")
                .points(8.0)
                .submitDate(LocalDate.of(2025, 6, 1))
                .build();

        when(quizRecordRepository.findAllBySubmitDateBetween(any(), any()))
                .thenReturn(List.of(quizRecord));

        Map<String, Map<String, Double>> result = statisticService.getQuizAverageByMonth(date, subjectName);

        assertNotNull(result);
        assertTrue(result.containsKey("01-06"));
        assertEquals(8.0, result.get("01-06").get("Lớp 1"));
        assertEquals(0.0, result.get("01-06").get("Lớp 2"));

        verify(quizRecordRepository).findAllBySubmitDateBetween(any(), any());
    }

    @Test
    void getQuizScoreStatisticsBySubject_Success() {
        QuizRecord quizRecord = QuizRecord.builder()
                .recordId("record1")
                .quiz(testQuiz)
                .points(8.0)
                .build();

        when(subjectRepository.findAll()).thenReturn(List.of(testSubject));
        when(quizRecordRepository.findAll()).thenReturn(List.of(quizRecord));

        Map<String, Map<String, Integer>> result = statisticService.getQuizScoreStatisticsBySubject();

        assertNotNull(result);
        assertTrue(result.containsKey("Math"));
        assertEquals(100, result.get("Math").get("8.0 - 10.0"));
        assertEquals(0, result.get("Math").get("0.0 - 3.4"));

        verify(subjectRepository).findAll();
        verify(quizRecordRepository).findAll();
    }

    @Test
    void getUsersJoinedBattleBySubject_Success() {
        Map<String, Object> aggregationResult = new HashMap<>();
        aggregationResult.put("subjectId", "sub1");
        aggregationResult.put("userCount", 10);

        when(subjectRepository.findAll()).thenReturn(List.of(testSubject));
        when(battleRepository.aggregateUsersBySubject())
                .thenReturn(new AggregationResults<>(List.of(aggregationResult), new Document()));

        Map<String, Double> result = statisticService.getUsersJoinedBattleBySubject();

        assertNotNull(result);
        assertEquals(100.0, result.get("Math"));

        verify(subjectRepository).findAll();
        verify(battleRepository).aggregateUsersBySubject();
    }

    @Test
    void getBattleScoreDistributionBySubject_Success() {
        PlayerScore playerScore = PlayerScore.builder()
                .userId("user1")
                .username("testUser")
                .score(75)
                .build();

        Battle battle = Battle.builder()
                .id("battle1")
                .subjectId("sub1")
                .status("ENDED")
                .playerScores(List.of(playerScore))
                .build();

        when(subjectRepository.findAll()).thenReturn(List.of(testSubject));
        when(battleRepository.findByStatus(eq("ENDED"))).thenReturn(List.of(battle));

        Map<String, Map<String, Integer>> result = statisticService.getBattleScoreDistributionBySubject();

        assertNotNull(result);
        assertTrue(result.containsKey("Math"));
        assertEquals(1, result.get("Math").get("71-90"));
        assertEquals(0, result.get("Math").get("0-50"));

        verify(subjectRepository).findAll();
        verify(battleRepository).findByStatus(eq("ENDED"));
    }

    @Test
    void getBattleAveragePointsByMonth_Success() {
        String date = "06-2025";
        String subjectName = "Math";

        PlayerScore playerScore = PlayerScore.builder()
                .userId("user1")
                .username("testUser")
                .score(75)
                .build();

        Battle battle = Battle.builder()
                .id("battle1")
                .subjectId("sub1")
                .gradeId("grade1")
                .startTime(LocalDate.from(LocalDate.of(2025, 6, 1).atStartOfDay()))
                .playerScores(List.of(playerScore))
                .build();

        when(subjectRepository.findAllById(anyIterable())).thenReturn(List.of(testSubject));
        when(gradeRepository.findAllById(anyIterable())).thenReturn(List.of(testGrade));
        when(battleRepository.findAllByStartTimeBetween(any(), any())).thenReturn(List.of(battle));

        Map<String, Map<String, Double>> result = statisticService.getBattleAveragePointsByMonth(date, subjectName);

        assertNotNull(result);
        assertTrue(result.containsKey("01-06"));
        assertEquals(75.0, result.get("01-06").get("Lớp 1"));
        assertEquals(0.0, result.get("01-06").get("Lớp 2"));

        verify(subjectRepository).findAllById(anyIterable());
        verify(gradeRepository).findAllById(anyIterable());
        verify(battleRepository).findAllByStartTimeBetween(any(), any());
    }
}