package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.config.BattleWebSocketHandler;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.QuestionType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.BattleUserResponse;
import com.api.bee_smart_backend.helper.response.UserWithBattleStatsResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.dto.BattleUser;
import com.api.bee_smart_backend.model.dto.PlayerScore;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.QuestionService;
import com.api.bee_smart_backend.service.impl.BattleServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplTest {

    @InjectMocks
    private BattleServiceImpl battleService;

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private BattleUserRepository battleUserRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private BattleWebSocketHandler webSocketHandler;

    @Mock
    private MapData mapData;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        battleService = new BattleServiceImpl(
                battleRepository, userRepository, tokenRepository,
                gradeRepository, subjectRepository, battleUserRepository,
                questionService, webSocketHandler, mapData
        );

        // Inject mocked scheduler using reflection
        Field schedulerField = BattleServiceImpl.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(battleService, scheduler);

        // Clear internal state for isolation
        battleService.matchmakingQueues.clear();
        battleService.battleQuestionNumbers.clear();
        battleService.questionAnswers.clear();
        battleService.timeoutTasks.clear();
    }

    @Test
    void matchPlayers_WhenQueueEmpty_ShouldAddPlayerToQueue() {
        BattleRequest request = BattleRequest.builder()
                .gradeId("grade1")
                .subjectId("subject1")
                .playerIds(List.of("user1"))
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> battleService.matchPlayers(request));
        assertEquals("Waiting for other players...", exception.getMessage());
        assertEquals(HttpStatus.ACCEPTED, exception.getStatus());
    }

    @Test
    void matchPlayers_WhenOpponentAvailable_ShouldCreateBattle() throws Exception {
        BattleRequest request = BattleRequest.builder()
                .gradeId("grade1")
                .subjectId("subject1")
                .playerIds(List.of("user1"))
                .topic("Math Battle")
                .build();

        User user1 = User.builder().userId("user1").username("User1").build();
        User user2 = User.builder().userId("user2").username("User2").build();
        Battle battle = Battle.builder()
                .id("battle1")
                .topic("Math Battle")
                .gradeId("grade1")
                .subjectId("subject1")
                .status("ONGOING")
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 0, 0, 0),
                        new PlayerScore("user2", "User2", 0, 0, 0)
                ))
                .startTime(LocalDate.now())
                .answeredQuestions(new HashSet<>())
                .createdAt(Instant.now())
                .build();
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(battleRepository.save(any(Battle.class))).thenReturn(battle);
        when(mapData.mapOne(battle, BattleResponse.class)).thenReturn(battleResponse);
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());
        when(battleUserRepository.findByUserAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        when(battleUserRepository.save(any(BattleUser.class))).thenReturn(new BattleUser());

        String queueKey = "grade1:subject1";
        Queue<String> queue = new LinkedList<>();
        queue.offer("user2");
        battleService.matchmakingQueues.put(queueKey, queue);

        BattleResponse response = battleService.matchPlayers(request);

        assertNotNull(response);
        assertEquals("battle1", response.getBattleId());
        verify(battleRepository).save(any(Battle.class));
        verify(webSocketHandler, times(2)).broadcastUpdate(eq("battle1"), anyString()); // Expect two calls
        verify(battleUserRepository, times(2)).save(any(BattleUser.class));
    }

    @Test
    void createBattle_InvalidPlayerCount_ShouldThrowException() {
        BattleRequest request = BattleRequest.builder()
                .playerIds(List.of("user1")) // Only one player
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> battleService.createBattle(request));
        assertEquals("Battle must have exactly 2 players!", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void createBattle_ValidRequest_ShouldCreateBattle() throws Exception {
        BattleRequest request = BattleRequest.builder()
                .topic("Math Battle")
                .gradeId("grade1")
                .subjectId("subject1")
                .playerIds(List.of("user1", "user2"))
                .build();

        User user1 = User.builder().userId("user1").username("User1").build();
        User user2 = User.builder().userId("user2").username("User2").build();
        Battle battle = Battle.builder()
                .id("battle1")
                .topic("Math Battle")
                .gradeId("grade1")
                .subjectId("subject1")
                .status("ONGOING")
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 0, 0, 0),
                        new PlayerScore("user2", "User2", 0, 0, 0)
                ))
                .startTime(LocalDate.now())
                .answeredQuestions(new HashSet<>())
                .createdAt(Instant.now())
                .build();
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(battleRepository.save(any(Battle.class))).thenReturn(battle);
        when(battleUserRepository.findByUserAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        when(battleUserRepository.save(any(BattleUser.class))).thenReturn(new BattleUser());
        when(mapData.mapOne(battle, BattleResponse.class)).thenReturn(battleResponse);
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());

        BattleResponse response = battleService.createBattle(request);

        assertNotNull(response);
        assertEquals("battle1", response.getBattleId());
        verify(battleRepository).save(any(Battle.class));
        verify(battleUserRepository, times(2)).save(any(BattleUser.class));
        verify(webSocketHandler).broadcastUpdate(eq("battle1"), anyString());
    }

    @Test
    void sendNextQuestion_BattleEnded_ShouldThrowException() throws Exception {
        Battle battle = Battle.builder()
                .id("battle1")
                .status("ENDED")
                .build();

        when(battleRepository.findById("battle1")).thenReturn(Optional.of(battle));
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());

        CustomException exception = assertThrows(CustomException.class, () -> battleService.sendNextQuestion("battle1"));
        assertEquals("Battle has already ended!", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(webSocketHandler).broadcastUpdate(eq("battle1"), anyString());
    }

    @Test
    void sendNextQuestion_AllQuestionsAnswered_ShouldEndBattle() throws Exception {
        Battle battle = Battle.builder()
                .id("battle1")
                .status("ONGOING")
                .gradeId("grade1")
                .subjectId("subject1")
                .answeredQuestions(new HashSet<>())
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 0, 0, 0),
                        new PlayerScore("user2", "User2", 0, 0, 0)
                ))
                .build();
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");

        when(battleRepository.findById("battle1")).thenReturn(Optional.of(battle));
        when(battleRepository.save(any(Battle.class))).thenReturn(battle);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(new User()));
        when(battleUserRepository.findByUserAndDeletedAtIsNull(any())).thenReturn(Optional.of(new BattleUser()));
        when(battleUserRepository.save(any(BattleUser.class))).thenReturn(new BattleUser());
        when(mapData.mapOne(any(Battle.class), eq(BattleResponse.class))).thenReturn(battleResponse);
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());

        battleService.battleQuestionNumbers.put("battle1", 10);

        CustomException exception = assertThrows(CustomException.class, () -> battleService.sendNextQuestion("battle1"));
        assertEquals("Battle has ended - all questions completed!", exception.getMessage());
        assertEquals(HttpStatus.OK, exception.getStatus());
        verify(battleRepository).save(any(Battle.class));
        verify(webSocketHandler).broadcastUpdate(eq("battle1"), anyString());
    }

    @Test
    void submitAnswer_BothPlayersAnswered_ShouldUpdateScoresAndSendNextQuestion() throws Exception {
        Battle battle = Battle.builder()
                .id("battle1")
                .status("ONGOING")
                .gradeId("grade1")
                .subjectId("subject1")
                .answeredQuestions(new HashSet<>())
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 0, 0, 0),
                        new PlayerScore("user2", "User2", 0, 0, 0)
                ))
                .build();
        AnswerRequest answer1 = new AnswerRequest("user1", "q1", "A", null, 10);
        AnswerRequest answer2 = new AnswerRequest("user2", "q1", "A", null, 15);
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");
        Question question = Question.builder()
                .questionId("q1")
                .content("What is 2+2?")
                .options(List.of("A", "B", "C", "D"))
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .build();

        when(battleRepository.findById("battle1")).thenReturn(Optional.of(battle));
        when(questionService.checkAnswer("q1", "A")).thenReturn(true);
        when(battleRepository.save(any(Battle.class))).thenReturn(battle);
        when(mapData.mapOne(battle, BattleResponse.class)).thenReturn(battleResponse);
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());
        when(questionService.getRandomQuestionByGradeAndSubject(anyString(), anyString(), anySet()))
                .thenReturn(question);
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), eq(31L), eq(TimeUnit.SECONDS));
        doReturn(true).when(scheduledFuture).cancel(true);

        // Initialize battleQuestionNumbers to prevent battle from ending
        battleService.battleQuestionNumbers.put("battle1", 1);

        BattleResponse response = battleService.submitAnswer("battle1", answer1);
        response = battleService.submitAnswer("battle1", answer2);

        assertNotNull(response);
        assertEquals("battle1", response.getBattleId());
        verify(questionService, times(2)).checkAnswer(eq("q1"), eq("A"));
        verify(battleRepository, times(2)).save(any(Battle.class));
        verify(webSocketHandler, times(2)).broadcastUpdate(eq("battle1"), anyString());
        verify(questionService).getRandomQuestionByGradeAndSubject(eq("grade1"), eq("subject1"), anySet());
        verify(scheduler).schedule(any(Runnable.class), eq(31L), eq(TimeUnit.SECONDS));
        verify(scheduledFuture).cancel(true);
    }

    @Test
    void endBattle_ValidBattleId_ShouldEndBattleAndUpdateStats() throws Exception {
        Battle battle = Battle.builder()
                .id("battle1")
                .status("ONGOING")
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 20, 2, 0),
                        new PlayerScore("user2", "User2", 10, 1, 1)
                ))
                .build();
        User user1 = User.builder().userId("user1").build();
        User user2 = User.builder().userId("user2").build();
        BattleUser battleUser1 = BattleUser.builder().user(user1).totalBattleWon(0).totalBattleLost(0).build();
        BattleUser battleUser2 = BattleUser.builder().user(user2).totalBattleWon(0).totalBattleLost(0).build();
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");

        when(battleRepository.findById("battle1")).thenReturn(Optional.of(battle));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(battleUserRepository.findByUserAndDeletedAtIsNull(user1)).thenReturn(Optional.of(battleUser1));
        when(battleUserRepository.findByUserAndDeletedAtIsNull(user2)).thenReturn(Optional.of(battleUser2));
        when(battleRepository.save(any(Battle.class))).thenReturn(battle);
        when(battleUserRepository.save(any(BattleUser.class))).thenReturn(battleUser1);
        when(mapData.mapOne(battle, BattleResponse.class)).thenReturn(battleResponse);
        doNothing().when(webSocketHandler).broadcastUpdate(anyString(), anyString());

        battleService.endBattle("battle1");

        verify(battleRepository).save(any(Battle.class));
        verify(battleUserRepository, times(2)).save(any(BattleUser.class));
        verify(webSocketHandler).broadcastUpdate(eq("battle1"), anyString());
        assertEquals("ENDED", battle.getStatus());
        assertEquals("user1", battle.getWinner());
    }

    @Test
    void getBattleById_BattleNotFound_ShouldThrowException() {
        when(battleRepository.findById("battle1")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> battleService.getBattleById("battle1"));
        assertEquals("Battle not found!", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getBattleById_ValidBattleId_ShouldReturnBattle() {
        Battle battle = Battle.builder().id("battle1").status("ONGOING").build();
        BattleResponse battleResponse = new BattleResponse();
        battleResponse.setBattleId("battle1");

        when(battleRepository.findById("battle1")).thenReturn(Optional.of(battle));
        when(mapData.mapOne(battle, BattleResponse.class)).thenReturn(battleResponse);

        BattleResponse response = battleService.getBattleById("battle1");

        assertNotNull(response);
        assertEquals("battle1", response.getBattleId());
        verify(battleRepository).findById("battle1");
    }

    @Test
    void checkMatchmakingStatus_QueueExists_ShouldReturnPlayerCount() {
        String queueKey = "grade1:subject1";
        Queue<String> queue = new LinkedList<>();
        queue.offer("user1");
        battleService.matchmakingQueues.put(queueKey, queue);

        String status = battleService.checkMatchmakingStatus("grade1", "subject1");
        assertEquals("Players in queue: 1", status);
    }

    @Test
    void getOnlineList_ValidToken_ShouldReturnOnlineUsers() {
        String jwtToken = "valid-token";
        Token token = Token.builder().accessToken(jwtToken).user(User.builder().userId("user1").build()).build();
        User user2 = User.builder().userId("user2").username("User2").isOnline(true).build();
        Page<User> userPage = new PageImpl<>(List.of(user2), PageRequest.of(0, 10), 1L);

        when(tokenRepository.findByAccessToken(jwtToken)).thenReturn(Optional.of(token));
        when(userRepository.findByIsOnlineTrueAndDeletedAtIsNullAndRoleNotAndUserIdNot(any(), anyString(), anyString()))
                .thenReturn(userPage);
        when(battleUserRepository.findByUserAndDeletedAtIsNull(user2))
                .thenReturn(Optional.of(BattleUser.builder().totalBattleWon(5).totalBattleLost(3).build()));

        Map<String, Object> response = battleService.getOnlineList(jwtToken, "0", "10", "");

        assertNotNull(response);
        assertEquals(1, response.get("totalPages")); // Expect Integer
        assertEquals(1, response.get("totalElements")); // Expect Integer
        List<UserWithBattleStatsResponse> users = (List<UserWithBattleStatsResponse>) response.get("users");
        assertEquals(1, users.size());
        assertEquals("user2", users.get(0).getUserId());
        assertEquals(5, users.get(0).getTotalBattleWon());
        assertEquals(3, users.get(0).getTotalBattleLost());
    }

    @Test
    void getBattleUserDetail_ValidToken_ShouldReturnUserDetails() {
        String jwtToken = "valid-token";
        User user = User.builder().userId("user1").username("User1").build();
        Token token = Token.builder().accessToken(jwtToken).user(user).build();
        BattleUser battleUser = BattleUser.builder()
                .battleUserId("bu1")
                .user(user)
                .totalBattleWon(5)
                .totalBattleLost(3)
                .build();
        Battle battle = Battle.builder()
                .id("battle1")
                .status("ENDED")
                .gradeId("grade1")
                .subjectId("subject1")
                .playerScores(List.of(
                        new PlayerScore("user1", "User1", 20, 2, 0),
                        new PlayerScore("user2", "User2", 10, 1, 1)
                ))
                .winner("user1")
                .updatedAt(Instant.now())
                .build();
        Grade grade = Grade.builder().gradeId("grade1").gradeName("Grade 1").build();
        Subject subject = Subject.builder().subjectId("subject1").subjectName("Math").build();
        Page<Battle> battlePage = new PageImpl<>(List.of(battle));

        when(tokenRepository.findByAccessToken(jwtToken)).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(battleUserRepository.findByUserAndDeletedAtIsNull(user)).thenReturn(Optional.of(battleUser));
        when(battleRepository.findByPlayerScoresUserIdAndStatus(eq("user1"), eq("ENDED"), any(Pageable.class)))
                .thenReturn(battlePage);
        when(gradeRepository.findById("grade1")).thenReturn(Optional.of(grade));
        when(subjectRepository.findById("subject1")).thenReturn(Optional.of(subject));

        BattleUserResponse response = battleService.getBattleUserDetail(jwtToken, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals("bu1", response.getBattleUserId());
        assertEquals("user1", response.getUserId());
        assertEquals("User1", response.getUsername());
        assertEquals(5, response.getTotalBattleWon());
        assertEquals(3, response.getTotalBattleLost());
        assertEquals(1, response.getHistoryResponses().size());
        assertEquals("battle1", response.getHistoryResponses().get(0).getBattleId());
        assertTrue(response.getHistoryResponses().get(0).isWon());
    }
}