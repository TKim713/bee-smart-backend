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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class BattleWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<WebSocketSession>> battleSessions = new ConcurrentHashMap<>();

    // Change to Map to handle grade/subject matchmaking queues
    private final Map<String, List<WebSocketSession>> matchmakingQueues = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        String userId = (String) attributes.get("userId");
        String battleId = (String) attributes.get("battleId");

        if (battleId == null) {
            // This is a matchmaking connection
            String gradeId = (String) attributes.get("gradeId");
            String subjectId = (String) attributes.get("subjectId");

            if (gradeId == null || subjectId == null) {
                session.close(CloseStatus.BAD_DATA.withReason("Missing grade or subject"));
                return;
            }

            // Create queue key based on grade and subject
            String queueKey = gradeId + ":" + subjectId;
            matchmakingQueues.computeIfAbsent(queueKey, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(session);

            log.info("User {} added to matchmaking queue for grade {} and subject {}. Total in queue: {}",
                    userId, gradeId, subjectId, matchmakingQueues.get(queueKey).size());

            // Check if we have enough players to start a match
            List<WebSocketSession> queue = matchmakingQueues.get(queueKey);
            if (queue.size() >= 2) {
                startMatchWithTwoPlayers(queueKey, gradeId, subjectId);
            } else {
                // Send status update to the player
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "STATUS",
                        "message", "Waiting for opponent..."
                ))));
            }
        } else {
            // This is a battle session connection
            battleSessions.computeIfAbsent(battleId, k -> new ArrayList<>()).add(session);
            log.info("User {} joined battle {}", userId, battleId);

            // Send confirmation to the user
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "JOINED",
                    "battleId", battleId
            ))));
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
            // Create a battle request
            BattleRequest battleRequest = BattleRequest.builder()
                    .topic(subjectId) // Using subject as the topic
                    .gradeId(gradeId)
                    .subjectId(subjectId)
                    .playerIds(List.of(userId1, userId2))
                    .build();

            BattleService battleService = applicationContext.getBean(BattleService.class);
            BattleResponse battle = battleService.createBattle(battleRequest);

            String battleId = battle.getBattleId();

            // Add these sessions to battle sessions
            battleSessions.put(battleId, List.of(player1, player2));

            // Send START message to both players
            String startMessage = objectMapper.writeValueAsString(Map.of(
                    "type", "START",
                    "battleId", battleId
            ));
            player1.sendMessage(new TextMessage(startMessage));
            player2.sendMessage(new TextMessage(startMessage));

            log.info("Battle created with ID: {} between users {} and {}", battleId, userId1, userId2);
        } catch (Exception e) {
            log.error("Error creating battle", e);
            try {
                player1.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "ERROR",
                        "message", "Failed to create battle, please try again"
                ))));
                player2.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "ERROR",
                        "message", "Failed to create battle, please try again"
                ))));
            } catch (IOException ex) {
                log.error("Error sending error message", ex);
            }
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

        // Remove from matchmaking queues
        for (Map.Entry<String, List<WebSocketSession>> entry : matchmakingQueues.entrySet()) {
            entry.getValue().remove(session);
        }

        // Handle player leaving a battle
        for (Map.Entry<String, List<WebSocketSession>> entry : battleSessions.entrySet()) {
            String battleId = entry.getKey();
            List<WebSocketSession> sessions = entry.getValue();

            if (sessions.remove(session)) {
                log.info("User {} left battle {}", userId, battleId);

                // If this was a battle in progress, notify other players and potentially end the battle
                if (!sessions.isEmpty()) {
                    try {
                        BattleService battleService = applicationContext.getBean(BattleService.class);
                        BattleResponse battle = battleService.getBattleById(battleId);

                        if ("ONGOING".equals(battle.getStatus())) {
                            // Player left during battle, notify others
                            broadcastUpdate(battleId, objectMapper.writeValueAsString(Map.of(
                                    "type", "PLAYER_LEFT",
                                    "userId", userId
                            )));

                            // End the battle if appropriate
                            if (sessions.size() < 2) {
                                battleService.endBattle(battleId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error handling player disconnect from battle", e);
                    }
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