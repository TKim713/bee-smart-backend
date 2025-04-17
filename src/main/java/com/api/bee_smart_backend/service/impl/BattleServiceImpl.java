package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.BattleWebSocketHandler;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.QuizResponse;
import com.api.bee_smart_backend.model.Battle;
import com.api.bee_smart_backend.model.Question;
import com.api.bee_smart_backend.model.dto.PlayerScore;
import com.api.bee_smart_backend.repository.BattleRepository;
import com.api.bee_smart_backend.service.BattleService;
import com.api.bee_smart_backend.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;
    private final QuestionService questionService;
    private final BattleWebSocketHandler webSocketHandler;
    private final MapData mapData;

    // Map structure: "{gradeId}:{subjectId}" -> Queue<String>
    private final Map<String, Queue<String>> matchmakingQueues = new ConcurrentHashMap<>();

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

        Battle battle = new Battle();
        battle.setTopic(request.getTopic());
        battle.setGradeId(request.getGradeId());
        battle.setSubjectId(request.getSubjectId());
        battle.setStatus("ONGOING");
        battle.setPlayerScores(request.getPlayerIds().stream()
                .map(id -> new PlayerScore(id, 0))
                .collect(Collectors.toList()));
        battle.setStartTime(Instant.now());
        battle.setAnsweredQuestions(new HashSet<>());

        Battle savedBattle = battleRepository.save(battle);
        BattleResponse response = mapData.mapOne(savedBattle, BattleResponse.class);

        // Send questions to players via WebSocket
        sendNextQuestion(savedBattle.getId());

        return response;
    }

    public void sendNextQuestion(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));

        if ("ENDED".equals(battle.getStatus())) {
            return;
        }

        // Get a random question based on grade and subject
        Question question = questionService.getRandomQuestionByGradeAndSubject(
                battle.getGradeId(),
                battle.getSubjectId(),
                battle.getAnsweredQuestions()
        );

        if (question == null) {
            // Try to get a question without excluding answered questions
            question = questionService.getRandomQuestionByGradeAndSubject(
                    battle.getGradeId(),
                    battle.getSubjectId(),
                    new HashSet<>()  // Empty set to get any question
            );

            if (question == null) {
                // Still no questions available, inform players and end the battle
                try {
                    Map<String, Object> errorMsg = new HashMap<>();
                    errorMsg.put("type", "ERROR");
                    errorMsg.put("message", "No questions available for this battle. Battle ended.");
                    webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(errorMsg));
                } catch (Exception e) {
                    log.error("Error sending error message via WebSocket", e);
                }

                // End the battle after informing players
                endBattle(battleId);
                return;
            } else {
                // Reset answered questions to allow reusing questions if needed
                battle.setAnsweredQuestions(new HashSet<>());
                battleRepository.save(battle);
            }
        }

        // Map the question to a format suitable for the client
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("questionId", question.getQuestionId());
        questionData.put("content", question.getContent());
        questionData.put("image", question.getImage());
        questionData.put("options", question.getOptions());

        Map<String, Object> questionMsg = new HashMap<>();
        questionMsg.put("type", "QUESTION");
        questionMsg.put("question", questionData);

        // Send the question to all players in the battle
        try {
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(questionMsg));
        } catch (Exception e) {
            log.error("Error sending question via WebSocket", e);
        }
    }

    @Override
    public BattleResponse submitAnswer(String battleId, AnswerRequest request) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Battle not found!", HttpStatus.NOT_FOUND));

        if ("ENDED".equals(battle.getStatus())) {
            throw new CustomException("The battle has ended!", HttpStatus.BAD_REQUEST);
        }

        if (battle.getAnsweredQuestions().contains(request.getQuestionId())) {
            throw new CustomException("This question has already been answered!", HttpStatus.CONFLICT);
        }

        boolean isCorrect = questionService.checkAnswer(request.getQuestionId(), request.getAnswer());
        updateScore(battle, request.getUserId(), isCorrect);
        battle.getAnsweredQuestions().add(request.getQuestionId());

        battleRepository.save(battle);
        BattleResponse updatedBattle = mapData.mapOne(battle, BattleResponse.class);

        // Send updated battle state via WebSocket
        try {
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(updatedBattle));
        } catch (Exception e) {
            log.error("Error sending battle update via WebSocket", e);
        }

        // Check if all players have answered
        boolean allPlayersAnswered = true;
        for (PlayerScore playerScore : battle.getPlayerScores()) {
            // Logic to check if this player has answered the current question
            // This may require additional tracking in your model
        }

        // If all players answered, send the next question after a short delay
        if (allPlayersAnswered) {
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                if (battle.getAnsweredQuestions().size() >= 10) {
                    endBattle(battleId);
                } else {
                    sendNextQuestion(battleId);
                }
            });
        }

        return updatedBattle;
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
        battle.setEndTime(Instant.now());

        Optional<PlayerScore> winner = battle.getPlayerScores().stream()
                .max(Comparator.comparingInt(PlayerScore::getScore));
        battle.setWinner(winner.map(PlayerScore::getUserId).orElse(null));

        battleRepository.save(battle);
        BattleResponse finalResponse = mapData.mapOne(battle, BattleResponse.class);

        // Send final results via WebSocket
        try {
            Map<String, Object> endMsg = new HashMap<>(new ObjectMapper().convertValue(finalResponse, Map.class));
            endMsg.put("type", "END");
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(endMsg));
        } catch (Exception e) {
            log.error("Error sending final results via WebSocket", e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutBattles() {
        Instant now = Instant.now();
        List<Battle> ongoing = battleRepository.findAllByStatus("ONGOING");

        for (Battle battle : ongoing) {
            if (battle.getStartTime() != null && Duration.between(battle.getStartTime(), now).toMinutes() > 10) {
                battle.setStatus("ENDED");
                battle.setEndTime(now);

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

    public void updateScore(Battle battle, String userId, boolean isCorrect) {
        int points = isCorrect ? 10 : 0;
        battle.getPlayerScores().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresentOrElse(
                        playerScore -> playerScore.setScore(playerScore.getScore() + points),
                        () -> battle.getPlayerScores().add(new PlayerScore(userId, points))
                );
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
}
