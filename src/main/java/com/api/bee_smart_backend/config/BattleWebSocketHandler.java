package com.api.bee_smart_backend.config;

import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.BattleWebSocketMessage;
import com.api.bee_smart_backend.service.BattleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class BattleWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> battleSessions = new ConcurrentHashMap<>();

    private final Map<String, List<WebSocketSession>> matchmakingQueues = new ConcurrentHashMap<>();

    private final Map<String, Boolean> battleReady = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        String userId = (String) attributes.get("userId");
        String battleId = (String) attributes.get("battleId");

        if (battleId == null) {
            String gradeId = (String) attributes.get("gradeId");
            String subjectId = (String) attributes.get("subjectId");

            if (gradeId == null || subjectId == null) {
                session.close(CloseStatus.BAD_DATA.withReason("Missing grade or subject"));
                return;
            }

            String queueKey = gradeId + ":" + subjectId;
            matchmakingQueues
                    .computeIfAbsent(queueKey, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(session);

            log.info("User {} added to matchmaking queue for grade {} and subject {}. Total in queue: {}",
                    userId, gradeId, subjectId, matchmakingQueues.get(queueKey).size());

            List<WebSocketSession> queue = matchmakingQueues.get(queueKey);
            if (queue.size() >= 2) {
                startMatchWithTwoPlayers(queueKey, gradeId, subjectId);
            } else {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "STATUS",
                        "message", "Waiting for opponent..."
                ))));
            }
        } else {
            CopyOnWriteArrayList<WebSocketSession> sessionList =
                    battleSessions.computeIfAbsent(battleId, k -> new CopyOnWriteArrayList<>());

            if (!sessionList.contains(session)) {
                sessionList.add(session);
                log.info("User {} reconnected to battle {}", userId, battleId);

                // Notify other players of reconnection
                broadcastUpdate(battleId, objectMapper.writeValueAsString(Map.of(
                        "type", "PLAYER_RECONNECTED",
                        "userId", userId,
                        "message", "Player reconnected"
                )));

                // Send current battle state to reconnected user
                BattleService battleService = applicationContext.getBean(BattleService.class);
                BattleResponse battle = battleService.getBattleById(battleId);

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "JOINED",
                        "battleId", battleId,
                        "battle", battle
                ))));

                if ("ONGOING".equals(battle.getStatus())) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(battle)));
                    // Mark battle as ready only when both players are connected
                    if (sessionList.size() >= 2 && !battleReady.getOrDefault(battleId, false)) {
                        battleReady.put(battleId, true);
                        battleService.sendNextQuestion(battleId);
                    }
                }
            }
        }
    }

    private synchronized void startMatchWithTwoPlayers(String queueKey, String gradeId, String subjectId) {
        List<WebSocketSession> queue = matchmakingQueues.get(queueKey);
        if (queue.size() < 2) return;

        WebSocketSession player1 = queue.remove(0);
        WebSocketSession player2 = queue.remove(0);

        String userId1 = (String) player1.getAttributes().get("userId");
        String userId2 = (String) player2.getAttributes().get("userId");

        try {
            BattleRequest battleRequest = BattleRequest.builder()
                    .topic(subjectId)
                    .gradeId(gradeId)
                    .subjectId(subjectId)
                    .playerIds(List.of(userId1, userId2))
                    .build();

            BattleService battleService = applicationContext.getBean(BattleService.class);
            BattleResponse battle = battleService.createBattle(battleRequest);

            String battleId = battle.getBattleId();

            CopyOnWriteArrayList<WebSocketSession> sessionList =
                    battleSessions.computeIfAbsent(battleId, k -> new CopyOnWriteArrayList<>());

            if (!sessionList.contains(player1)) sessionList.add(player1);
            if (!sessionList.contains(player2)) sessionList.add(player2);

            String startMessage = objectMapper.writeValueAsString(Map.of(
                    "type", "START",
                    "battleId", battleId
            ));

            player1.sendMessage(new TextMessage(startMessage));
            player2.sendMessage(new TextMessage(startMessage));

            // Initialize battle as not ready until both players confirm connection
            battleReady.put(battleId, false);

            log.info("Battle created with ID: {} between users {} and {}", battleId, userId1, userId2);
        } catch (Exception e) {
            log.error("Error creating battle", e);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        BattleWebSocketMessage msg = objectMapper.readValue(message.getPayload(), BattleWebSocketMessage.class);
        String userId = (String) session.getAttributes().get("userId");

        if ("answer".equals(msg.getType())) {
            AnswerRequest answerRequest = new AnswerRequest(
                    userId,
                    msg.getQuestionId(),
                    msg.getAnswer(),
                    msg.getAnswers(),
                    msg.getTimeTaken()
            );

            BattleService battleService = applicationContext.getBean(BattleService.class);
            BattleResponse updatedBattle = battleService.submitAnswer(msg.getBattleId(), answerRequest);

            String response = objectMapper.writeValueAsString(updatedBattle);
            broadcastUpdate(msg.getBattleId(), response);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String battleId = (String) session.getAttributes().get("battleId");

        matchmakingQueues.values().forEach(queue -> queue.removeIf(s -> s.getId().equals(session.getId())));

        if (battleId != null) {
            CopyOnWriteArrayList<WebSocketSession> sessions = battleSessions.get(battleId);
            if (sessions != null) {
                sessions.remove(session);
                log.info("User {} disconnected from battle {}", userId, battleId);

                try {
                    BattleService battleService = applicationContext.getBean(BattleService.class);
                    BattleResponse battle = battleService.getBattleById(battleId);

                    if ("ONGOING".equals(battle.getStatus())) {
                        broadcastUpdate(battleId, objectMapper.writeValueAsString(Map.of(
                                "type", "PLAYER_DISCONNECTED",
                                "userId", userId,
                                "message", "Player disconnected, waiting for reconnection..."
                        )));

                        // Schedule battle end after 60 seconds if the player doesn't reconnect
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                List<WebSocketSession> currentSessions = battleSessions.get(battleId);
                                if (currentSessions != null && !currentSessions.stream().anyMatch(s -> userId.equals(s.getAttributes().get("userId")))) {
                                    try {
                                        // Declare the remaining player as the winner
                                        battleService.endBattleWithWinner(battleId, userId);
                                        log.info("Battle {} ended due to player {} failing to reconnect", battleId, userId);
                                    } catch (Exception e) {
                                        log.error("Error ending battle after reconnection timeout", e);
                                    }
                                }
                            }
                        }, 60000); // 60-second reconnection grace period
                    }
                } catch (Exception e) {
                    log.error("Error handling player disconnect from battle", e);
                }
            }
        }
    }

    public void broadcastUpdate(String battleId, String message) {
        List<WebSocketSession> sessions = battleSessions.get(battleId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (Exception e) {
                    log.error("Error sending message to session", e);
                }
            }
        }
    }
}
