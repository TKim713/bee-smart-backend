package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.BattleWebSocketHandler;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.*;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.model.dto.BattleUser;
import com.api.bee_smart_backend.model.dto.PlayerScore;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.BattleService;
import com.api.bee_smart_backend.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BattleServiceImpl implements BattleService {

    @Autowired
    private final BattleRepository battleRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final GradeRepository gradeRepository;
    @Autowired
    private final SubjectRepository subjectRepository;
    @Autowired
    private final BattleUserRepository battleUserRepository;

    private final QuestionService questionService;
    private final BattleWebSocketHandler webSocketHandler;
    private final MapData mapData;

    // Map structure: "{gradeId}:{subjectId}" -> Queue<String>
    private final Map<String, Queue<String>> matchmakingQueues = new ConcurrentHashMap<>();
    private final Map<String, Integer> battleQuestionNumbers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> battleAnsweredPlayers = new ConcurrentHashMap<>();
    // Temporarily stores answers until all players answer a question
    private final Map<String, List<AnswerRequest>> questionAnswers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @Override
    public BattleResponse matchPlayers(BattleRequest request) {
        String gradeId = request.getGradeId();
        String subjectId = request.getSubjectId();
        String currentPlayer = request.getPlayerIds().get(0); // Only one player

        // Create queue key based on grade and subject
        String queueKey = gradeId + ":" + subjectId;
        matchmakingQueues.putIfAbsent(queueKey, new LinkedList<>());
        Queue<String> queue = matchmakingQueues.get(queueKey);

        synchronized (queue) {
            if (!queue.isEmpty()) {
                String opponent = queue.poll();

                BattleRequest battleRequest = BattleRequest.builder()
                        .topic(request.getTopic())
                        .gradeId(gradeId)
                        .subjectId(subjectId)
                        .playerIds(List.of(currentPlayer, opponent))
                        .build();

                BattleResponse battleResponse = createBattle(battleRequest);

                // Send WebSocket message to both players that the battle has started
                try {
                    webSocketHandler.broadcastUpdate(
                            battleResponse.getBattleId(),
                            new ObjectMapper().writeValueAsString(
                                    Map.of(
                                            "type", "START",
                                            "battleId", battleResponse.getBattleId()
                                    ))
                    );
                } catch (Exception e) {
                    log.error("Error sending WebSocket battle start message", e);
                }

                return battleResponse;
            } else {
                queue.offer(currentPlayer);
                throw new CustomException("Waiting for other players...", HttpStatus.ACCEPTED);
            }
        }
    }

    @Override
    public Map<String, Object> getAllBattles(String page, String size, String search) {
        Pageable pageable = PageRequest.of(
                (page != null) ? Integer.parseInt(page) : 0,
                (size != null) ? Integer.parseInt(size) : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Battle> battles = battleRepository.findByTopicContainingIgnoreCase(search, pageable);
        return Map.of(
                "battles", battles.getContent(),
                "totalPages", battles.getTotalPages(),
                "totalElements", battles.getTotalElements()
        );
    }

    @Override
    public BattleResponse createBattle(BattleRequest request) {
        if (request.getPlayerIds() == null || request.getPlayerIds().size() != 2) {
            throw new CustomException("Battle must have exactly 2 players!", HttpStatus.BAD_REQUEST);
        }

        // Create PlayerScore list for Battle
        List<PlayerScore> playerScores = request.getPlayerIds().stream().map(id -> {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new CustomException("User not found with ID: " + id, HttpStatus.NOT_FOUND));
            return new PlayerScore(user.getUserId(), user.getUsername(), 0, 0, 0);
        }).collect(Collectors.toList());

        // Create Battle
        Battle battle = Battle.builder()
                .topic(request.getTopic())
                .gradeId(request.getGradeId())
                .subjectId(request.getSubjectId())
                .status("ONGOING")
                .playerScores(playerScores)
                .startTime(LocalDate.now(ZoneId.systemDefault()))
                .answeredQuestions(new HashSet<>())
                .createdAt(Instant.now())
                .build();

        Battle savedBattle = battleRepository.save(battle);

        // Initialize or ensure BattleUser records for each player
        for (String userId : request.getPlayerIds()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException("User not found with ID: " + userId, HttpStatus.NOT_FOUND));
            Optional<BattleUser> existingBattleUser = battleUserRepository.findByUserAndDeletedAtIsNull(user);
            if (existingBattleUser.isEmpty()) {
                BattleUser battleUser = BattleUser.builder()
                        .user(user)
                        .totalBattleWon(0)
                        .totalBattleLost(0)
                        .createdAt(Instant.now())
                        .build();
                battleUserRepository.save(battleUser);
            }
        }

        battleQuestionNumbers.put(savedBattle.getId(), 0);

        // Send WebSocket message to both players that the battle has started
        try {
            webSocketHandler.broadcastUpdate(
                    savedBattle.getId(),
                    new ObjectMapper().writeValueAsString(
                            Map.of(
                                    "type", "START",
                                    "battleId", savedBattle.getId(),
                                    "playerScores", savedBattle.getPlayerScores()
                            ))
            );
        } catch (Exception e) {
            log.error("Error sending WebSocket battle start message", e);
        }

        return mapData.mapOne(savedBattle, BattleResponse.class);
    }

    @Override
    public BattleResponse sendNextQuestion(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));

        if ("ENDED".equals(battle.getStatus())) {
            log.warn("Attempted to send a question to ended battle: {}", battleId);

            try {
                Map<String, Object> endMsg = new HashMap<>();
                endMsg.put("type", "END");
                endMsg.put("battleId", battleId);
                endMsg.put("winner", battle.getWinner());
                webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(endMsg));
            } catch (Exception e) {
                log.error("Error sending battle ended notification", e);
            }

            throw new CustomException("Battle has already ended!", HttpStatus.BAD_REQUEST);
        }

        int currentQuestionNumber = battleQuestionNumbers.getOrDefault(battleId, 0);

        if (currentQuestionNumber >= 10) {
            endBattle(battleId);
            throw new CustomException("Battle has ended - all questions completed!", HttpStatus.OK);
        }

        // Get a question
        Question question = questionService.getRandomQuestionByGradeAndSubject(
                battle.getGradeId(),
                battle.getSubjectId(),
                battle.getAnsweredQuestions()
        );

        if (question == null) {
            try {
                Map<String, Object> errorMsg = new HashMap<>();
                errorMsg.put("type", "ERROR");
                errorMsg.put("message", "Not enough questions available for this battle. Battle ended.");
                webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(errorMsg));
            } catch (Exception e) {
                log.error("Error sending error message via WebSocket", e);
            }

            endBattle(battleId);
            throw new CustomException("Not enough questions available for this battle. Battle ended.", HttpStatus.NOT_FOUND);
        }

        // Increment after confirming question exists
        battleQuestionNumbers.put(battleId, currentQuestionNumber + 1);

        // Send question to frontend
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("questionId", question.getQuestionId());
        questionData.put("content", question.getContent());
        questionData.put("image", question.getImage());
        questionData.put("options", question.getOptions());

        Map<String, Object> questionMsg = new HashMap<>();
        questionMsg.put("type", question.getQuestionType().toString());
        questionMsg.put("question", questionData);
        questionMsg.put("currentQuestion", currentQuestionNumber + 1); // 1-based for FE
        questionMsg.put("totalQuestions", 10);

        try {
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(questionMsg));
        } catch (Exception e) {
            log.error("Error sending question via WebSocket", e);
        }

        return mapData.mapOne(battle, BattleResponse.class);
    }

    @Override
    public BattleResponse submitAnswer(String battleId, AnswerRequest request) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));

        if ("ENDED".equals(battle.getStatus())) {
            throw new CustomException("The battle has ended!", HttpStatus.BAD_REQUEST);
        }

        String questionId = request.getQuestionId();
        String key = battleId + ":" + questionId;

        questionAnswers.putIfAbsent(key, new ArrayList<>());
        List<AnswerRequest> answers = questionAnswers.get(key);

        // Prevent duplicate answers
        if (answers.stream().anyMatch(a -> a.getUserId().equals(request.getUserId()))) {
            throw new CustomException("You have already answered this question!", HttpStatus.CONFLICT);
        }

        // If the answer is multi-select, convert to comma-separated string
        if (request.getAnswers() != null && !request.getAnswers().isEmpty()) {
            String joinedAnswers = String.join(",", request.getAnswers());
            request.setAnswer(joinedAnswers);
        }

        answers.add(request);

        // If only 1 player answered, start timeout for the other
        if (answers.size() == 1 && battle.getPlayerScores().size() > 1) {
            ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
                List<String> answeredUsers = answers.stream()
                        .map(AnswerRequest::getUserId)
                        .toList();

                Optional<String> otherUserId = battle.getPlayerScores().stream()
                        .map(PlayerScore::getUserId)
                        .filter(userId -> !answeredUsers.contains(userId))
                        .findFirst();

                otherUserId.ifPresent(userId -> {
                    AnswerRequest timeoutAnswer = new AnswerRequest();
                    timeoutAnswer.setUserId(userId);
                    timeoutAnswer.setQuestionId(request.getQuestionId());
                    timeoutAnswer.setAnswer(null);
                    timeoutAnswer.setTimeTaken(30);

                    submitAnswer(battleId, timeoutAnswer);
                });
            }, 31, TimeUnit.SECONDS);

            timeoutTasks.put(key, timeoutTask);
        }

        if (answers.size() == battle.getPlayerScores().size()) {
            // Cancel timeout task if it exists
            if (timeoutTasks.containsKey(key)) {
                timeoutTasks.get(key).cancel(true);
                timeoutTasks.remove(key);
            }

            // Score and continue
            applyScoringLogic(battle, answers);
            questionAnswers.remove(key);
            battle.getAnsweredQuestions().add(questionId);
            battleRepository.save(battle);

            BattleResponse updatedBattle = mapData.mapOne(battle, BattleResponse.class);

            // Include PlayerScore in WebSocket message
            try {
                Map<String, Object> updateMsg = new ObjectMapper().convertValue(updatedBattle, Map.class);
                updateMsg.put("type", "SCORE_UPDATE");
                updateMsg.put("playerScores", battle.getPlayerScores());
                webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(updateMsg));
            } catch (Exception e) {
                log.error("Error sending battle update via WebSocket", e);
            }

            // Proceed to next question
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                if (battleQuestionNumbers.getOrDefault(battleId, 0) >= 10) {
                    endBattle(battleId);
                } else {
                    sendNextQuestion(battleId);
                }
            });

            return updatedBattle;
        }

        battleRepository.save(battle);
        return mapData.mapOne(battle, BattleResponse.class);
    }

    @Override
    public BattleResponse getBattleById(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));
        return mapData.mapOne(battle, BattleResponse.class);
    }

    @Override
    public void endBattle(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));

        battle.setStatus("ENDED");
        battle.setEndTime(LocalDate.now(ZoneId.systemDefault()));
        battle.setUpdatedAt(Instant.now());
        battleQuestionNumbers.remove(battleId);

        // Determine winner based on PlayerScore
        Optional<PlayerScore> winnerScore = battle.getPlayerScores().stream()
                .max(Comparator.comparingInt(PlayerScore::getScore));
        String winnerId = winnerScore.map(PlayerScore::getUserId).orElse(null);

        // Update BattleUser records
        for (PlayerScore playerScore : battle.getPlayerScores()) {
            User user = userRepository.findById(playerScore.getUserId())
                    .orElseThrow(() -> new CustomException("User not found with ID: " + playerScore.getUserId(), HttpStatus.NOT_FOUND));
            Optional<BattleUser> battleUserOpt = battleUserRepository.findByUserAndDeletedAtIsNull(user);
            BattleUser battleUser = battleUserOpt.orElseGet(() -> {
                BattleUser newBattleUser = BattleUser.builder()
                        .user(user)
                        .totalBattleWon(0)
                        .totalBattleLost(0)
                        .createdAt(Instant.now())
                        .build();
                return battleUserRepository.save(newBattleUser);
            });

            if (playerScore.getUserId().equals(winnerId)) {
                battleUser.setTotalBattleWon(battleUser.getTotalBattleWon() + 1);
            } else {
                battleUser.setTotalBattleLost(battleUser.getTotalBattleLost() + 1);
            }
            battleUserRepository.save(battleUser);
        }

        // Update Battle
        battle.setWinner(winnerId);
        battleRepository.save(battle);

        BattleResponse finalResponse = mapData.mapOne(battle, BattleResponse.class);

        // Include PlayerScore in WebSocket message
        try {
            Map<String, Object> endMsg = new HashMap<>(new ObjectMapper().convertValue(finalResponse, Map.class));
            endMsg.put("type", "END");
            endMsg.put("playerScores", battle.getPlayerScores());
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(endMsg));
        } catch (Exception e) {
            log.error("Error sending final results via WebSocket", e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutBattles() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<Battle> ongoing = battleRepository.findAllByStatus("ONGOING");

        for (Battle battle : ongoing) {
            if (battle.getStartTime() != null && Duration.between(
                    battle.getStartTime().atStartOfDay(ZoneId.systemDefault()),
                    today.atStartOfDay(ZoneId.systemDefault())
            ).toDays() >= 1) { // Changed to compare days
                battle.setStatus("ENDED");
                battle.setEndTime(today); // Updated to LocalDate

                Optional<PlayerScore> winner = battle.getPlayerScores().stream()
                        .max(Comparator.comparingInt(PlayerScore::getScore));
                battle.setWinner(winner.map(PlayerScore::getUserId).orElse(null));

                battleRepository.save(battle);
                BattleResponse response = mapData.mapOne(battle, BattleResponse.class);

                // Send timeout update via WebSocket
                try {
                    Map<String, Object> timeoutMsg = new HashMap<>(new ObjectMapper().convertValue(response, Map.class));
                    timeoutMsg.put("type", "END");
                    timeoutMsg.put("reason", "TIMEOUT");
                    webSocketHandler.broadcastUpdate(battle.getId(), new ObjectMapper().writeValueAsString(timeoutMsg));
                } catch (Exception e) {
                    log.error("Error sending timeout update via WebSocket", e);
                }
            }
        }
    }

    @Override
    public String checkMatchmakingStatus(String gradeId, String subjectId) {
        String queueKey = gradeId + ":" + subjectId;
        Queue<String> queue = matchmakingQueues.get(queueKey);
        if (queue == null) {
            return "No queue exists for this grade and subject.";
        }
        return "Players in queue: " + queue.size();
    }

    private void applyScoringLogic(Battle battle, List<AnswerRequest> answers) {
        if (answers == null || answers.isEmpty()) return;

        // If only one player answered
        if (answers.size() == 1) {
            AnswerRequest only = answers.get(0);
            boolean correct = questionService.checkAnswer(only.getQuestionId(), only.getAnswer());
            int points = correct ? 10 : 0;

            // Update PlayerScore
            battle.getPlayerScores().stream()
                    .filter(p -> p.getUserId().equals(only.getUserId()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setScore(p.getScore() + points);
                        if (correct) {
                            p.setCorrectAnswers(p.getCorrectAnswers() + 1);
                        } else {
                            p.setIncorrectAnswers(p.getIncorrectAnswers() + 1);
                        }
                    });

            return;
        }

        // If two players answered
        answers.sort(Comparator.comparingInt(AnswerRequest::getTimeTaken)); // Faster first
        boolean firstCorrect = questionService.checkAnswer(answers.get(0).getQuestionId(), answers.get(0).getAnswer());

        for (int i = 0; i < answers.size(); i++) {
            AnswerRequest req = answers.get(i);
            boolean isCorrect = questionService.checkAnswer(req.getQuestionId(), req.getAnswer());

            int points;
            if (isCorrect) {
                if (i == 0) {
                    points = 10; // First and correct
                } else if (!firstCorrect) {
                    points = 10; // Second and correct, first was wrong
                } else {
                    points = 5; // Second and correct, but first was also correct
                }
            } else {
                points = 0;
            }

            // Update PlayerScore
            battle.getPlayerScores().stream()
                    .filter(p -> p.getUserId().equals(req.getUserId()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setScore(p.getScore() + points);
                        if (isCorrect) {
                            p.setCorrectAnswers(p.getCorrectAnswers() + 1);
                        } else {
                            p.setIncorrectAnswers(p.getIncorrectAnswers() + 1);
                        }
                    });
        }
    }

    @Override
    public Map<String, Object> getOnlineList(String jwtToken, String page, String size, String search) {
        try {
            // Extract userId from JWT token
            Token token = tokenRepository.findByAccessToken(jwtToken)
                    .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
            String currentUserId = token.getUser().getUserId();

            int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
            int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

            Page<User> users;
            if (search != null && !search.isBlank()) {
                users = userRepository.findByUsernameContainingIgnoreCaseAndIsOnlineTrueAndDeletedAtIsNullAndRoleNotAndUserIdNot(
                        search, pageable, "SYSTEM_ADMIN", currentUserId);
            } else {
                users = userRepository.findByIsOnlineTrueAndDeletedAtIsNullAndRoleNotAndUserIdNot(pageable, "SYSTEM_ADMIN", currentUserId);
            }

            List<UserWithBattleStatsResponse> userWithStatsList = users.getContent().stream().map(user -> {
                // Fetch BattleUser for battle stats
                Optional<BattleUser> battleUserOpt = battleUserRepository.findByUserAndDeletedAtIsNull(user);
                int totalBattleWon = battleUserOpt.map(BattleUser::getTotalBattleWon).orElse(0);
                int totalBattleLost = battleUserOpt.map(BattleUser::getTotalBattleLost).orElse(0);

                // Create UserWithBattleStatsResponse
                return UserWithBattleStatsResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .totalBattleWon(totalBattleWon)
                        .totalBattleLost(totalBattleLost)
                        .build();
            }).collect(Collectors.toList());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalPages", users.getTotalPages());
            response.put("totalElements", users.getTotalElements());
            response.put("users", userWithStatsList);
            return response;
        } catch (Exception e) {
            log.error("Error retrieving online list: {}", e.getMessage());
            throw new CustomException("Error retrieving online list: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BattleUserResponse getBattleUserDetail(String jwtToken, Pageable pageable) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        // Fetch BattleUser
        BattleUser battleUser = battleUserRepository.findByUserAndDeletedAtIsNull(user)
                .orElseGet(() -> {
                    BattleUser newBattleUser = BattleUser.builder()
                            .user(user)
                            .totalBattleWon(0)
                            .totalBattleLost(0)
                            .build();
                    return battleUserRepository.save(newBattleUser);
                });

        // Create Pageable with sorting by updatedAt in descending order
        Pageable updatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        // Fetch battle history
        Page<Battle> battles = battleRepository.findByPlayerScoresUserIdAndStatus(user.getUserId(), "ENDED", updatedPageable);

        List<BattleHistoryResponse> historyResponses = battles.getContent().stream().map(battle -> {
            // Get user's PlayerScore
            PlayerScore userScore = battle.getPlayerScores().stream()
                    .filter(ps -> ps.getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("PlayerScore not found for user", HttpStatus.NOT_FOUND));

            // Get opponent's PlayerScore
            PlayerScore opponentScore = battle.getPlayerScores().stream()
                    .filter(ps -> !ps.getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException("Opponent PlayerScore not found", HttpStatus.NOT_FOUND));

            // Fetch Grade and Subject
            Grade grade = gradeRepository.findById(battle.getGradeId())
                    .orElseThrow(() -> new CustomException("Grade not found with ID: " + battle.getGradeId(), HttpStatus.NOT_FOUND));
            Subject subject = subjectRepository.findById(battle.getSubjectId())
                    .orElseThrow(() -> new CustomException("Subject not found with ID: " + battle.getSubjectId(), HttpStatus.NOT_FOUND));

            // Determine if user won
            boolean won = battle.getWinner() != null && battle.getWinner().equals(user.getUserId());

            return BattleHistoryResponse.builder()
                    .battleId(battle.getId())
                    .opponentUsername(opponentScore.getUsername())
                    .gradeName(grade.getGradeName())
                    .subjectName(subject.getSubjectName())
                    .topic(battle.getTopic())
                    .won(won)
                    .score(userScore.getScore())
                    .correctAnswers(userScore.getCorrectAnswers())
                    .incorrectAnswers(userScore.getIncorrectAnswers())
                    .completedAt(battle.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());

        return BattleUserResponse.builder()
                .battleUserId(battleUser.getBattleUserId())
                .userId(battleUser.getUser().getUserId())
                .username(battleUser.getUser().getUsername())
                .totalBattleWon(battleUser.getTotalBattleWon())
                .totalBattleLost(battleUser.getTotalBattleLost())
                .historyResponses(historyResponses)
                .build();
    }
}