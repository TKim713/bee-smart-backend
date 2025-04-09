package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.BattleWebSocketHandler;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.model.Battle;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;
    private final QuestionService questionService;
    private final BattleWebSocketHandler webSocketHandler;

    private final Map<String, Queue<String>> matchmakingQueues = new ConcurrentHashMap<>();

    @Override
    public BattleResponse matchPlayers(BattleRequest request) {
        String topic = request.getTopic();
        String currentPlayer = request.getPlayerIds().get(0); // chỉ có 1 player

        matchmakingQueues.putIfAbsent(topic, new LinkedList<>());
        Queue<String> queue = matchmakingQueues.get(topic);

        synchronized (queue) {
            if (!queue.isEmpty()) {
                String opponent = queue.poll();

                BattleRequest battleRequest = BattleRequest.builder()
                        .topic(topic)
                        .playerIds(List.of(currentPlayer, opponent))
                        .build();

                BattleResponse battleResponse = createBattle(battleRequest);

                // 🔁 Gửi WebSocket cho cả hai player rằng trận đấu đã bắt đầu
                try {
                    webSocketHandler.broadcastUpdate(
                            battleResponse.getBattleId(),
                            new ObjectMapper().writeValueAsString(battleResponse)
                    );
                } catch (Exception e) {
                    log.error("Lỗi khi gửi WebSocket bắt đầu trận đấu", e);
                }

                return battleResponse;
            } else {
                queue.offer(currentPlayer);
                throw new CustomException("Đang chờ người chơi khác...", HttpStatus.ACCEPTED);
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
            throw new CustomException("Trận đấu phải có đúng 2 người chơi!", HttpStatus.BAD_REQUEST);
        }

        Battle battle = new Battle();
        battle.setTopic(request.getTopic());
        battle.setStatus("ONGOING");
        battle.setPlayerScores(request.getPlayerIds().stream()
                .map(id -> new PlayerScore(id, 0))
                .collect(Collectors.toList()));
        battle.setStartTime(Instant.now());
        battle.setAnsweredQuestions(new HashSet<>());

        Battle savedBattle = battleRepository.save(battle);
        BattleResponse response = convertToResponse(savedBattle);

        // 🟢 Gửi thông báo tạo trận qua WebSocket
        try {
            webSocketHandler.broadcastUpdate(savedBattle.getId(), new ObjectMapper().writeValueAsString(response));
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo tạo trận qua WebSocket", e);
        }

        return response;
    }

    @Override
    public BattleResponse submitAnswer(String battleId, AnswerRequest request) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Trận đấu không tồn tại!", HttpStatus.NOT_FOUND));

        if ("ENDED".equals(battle.getStatus())) {
            throw new CustomException("Trận đấu đã kết thúc!", HttpStatus.BAD_REQUEST);
        }

        if (battle.getAnsweredQuestions().contains(request.getQuestionId())) {
            throw new CustomException("Câu hỏi này đã được trả lời!", HttpStatus.CONFLICT);
        }

        boolean isCorrect = questionService.checkAnswer(request.getQuestionId(), request.getAnswer());
        updateScore(battle, request.getUserId(), isCorrect);
        battle.getAnsweredQuestions().add(request.getQuestionId());

        battleRepository.save(battle);
        BattleResponse updatedBattle = convertToResponse(battle);

        // 🔄 Gửi kết quả cập nhật qua WebSocket
        try {
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(updatedBattle));
        } catch (Exception e) {
            log.error("Lỗi khi gửi cập nhật trận đấu qua WebSocket", e);
        }

        return updatedBattle;
    }

    @Override
    public BattleResponse getBattleById(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Trận đấu không tồn tại!", HttpStatus.NOT_FOUND));
        return convertToResponse(battle);
    }

    @Override
    public void endBattle(String battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException("Trận đấu không tồn tại!", HttpStatus.NOT_FOUND));

        battle.setStatus("ENDED");
        battle.setEndTime(Instant.now());

        Optional<PlayerScore> winner = battle.getPlayerScores().stream()
                .max(Comparator.comparingInt(PlayerScore::getScore));
        battle.setWinner(winner.map(PlayerScore::getUserId).orElse(null));

        battleRepository.save(battle);
        BattleResponse finalResponse = convertToResponse(battle);

        // 🏁 Gửi kết quả cuối cùng qua WebSocket
        try {
            webSocketHandler.broadcastUpdate(battleId, new ObjectMapper().writeValueAsString(finalResponse));
        } catch (Exception e) {
            log.error("Lỗi khi gửi kết quả cuối cùng qua WebSocket", e);
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
                BattleResponse response = convertToResponse(battle);

                // ⏱ Gửi cập nhật timeout qua WebSocket
                try {
                    webSocketHandler.broadcastUpdate(battle.getId(), new ObjectMapper().writeValueAsString(response));
                } catch (Exception e) {
                    log.error("Lỗi khi gửi cập nhật timeout qua WebSocket", e);
                }
            }
        }
    }

    private BattleResponse convertToResponse(Battle battle) {
        return new BattleResponse(
                battle.getId(),
                battle.getTopic(),
                battle.getStatus(),
                battle.getPlayerScores(),
                battle.getWinner()
        );
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
    public String checkMatchmakingStatus(String topic) {
        Queue<String> queue = matchmakingQueues.get(topic);
        if (queue == null) {
            return "Không có hàng đợi nào cho chủ đề này.";
        }
        return "Số người chơi trong hàng đợi: " + queue.size();
    }
}
