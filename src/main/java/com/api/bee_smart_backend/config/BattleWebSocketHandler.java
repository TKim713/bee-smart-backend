package com.api.bee_smart_backend.config;

import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.BattleWebSocketMessage;
import com.api.bee_smart_backend.service.BattleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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
    private final List<WebSocketSession> matchmakingQueue = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();

        if (query == null || !query.contains("battleId")) {
            matchmakingQueue.add(session);
            log.info("User added to matchmaking queue. Total: {}", matchmakingQueue.size());

            if (matchmakingQueue.size() >= 2) {
                WebSocketSession player1 = matchmakingQueue.remove(0);
                WebSocketSession player2 = matchmakingQueue.remove(0);

                String userId1 = extractQueryParam(player1.getUri().getQuery(), "userId");
                String userId2 = extractQueryParam(player2.getUri().getQuery(), "userId");

                // ✅ Tạo BattleRequest
                BattleRequest battleRequest = BattleRequest.builder()
                        .topic("Math") // hoặc bạn có thể cho client gửi topic lên sau
                        .playerIds(List.of(userId1, userId2))
                        .build();

                BattleService battleService = applicationContext.getBean(BattleService.class);
                BattleResponse battle = battleService.createBattle(battleRequest); // ✅ gọi đúng

                String battleId = battle.getBattleId();
                battleSessions.put(battleId, List.of(player1, player2));

                // Gửi START cho 2 người chơi
                String startMessage = objectMapper.writeValueAsString(Map.of(
                        "type", "START",
                        "battleId", battleId
                ));
                player1.sendMessage(new TextMessage(startMessage));
                player2.sendMessage(new TextMessage(startMessage));

                log.info("Battle created with ID: {}", battleId);
            }

        } else {
            // Đã có battleId => tham gia phòng chơi
            String battleId = extractQueryParam(query, "battleId");
            battleSessions.computeIfAbsent(battleId, k -> new ArrayList<>()).add(session);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        BattleWebSocketMessage msg = objectMapper.readValue(message.getPayload(), BattleWebSocketMessage.class);

        if ("answer".equals(msg.getType())) {
            AnswerRequest answerRequest = new AnswerRequest(msg.getUserId(), msg.getQuestionId(), msg.getAnswer(), msg.getTimeTaken());

            BattleService battleService = applicationContext.getBean(BattleService.class);
            BattleResponse updatedBattle = battleService.submitAnswer(msg.getBattleId(), answerRequest);

            String response = objectMapper.writeValueAsString(updatedBattle);
            broadcastUpdate(msg.getBattleId(), response);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Xóa khỏi matchmaking queue nếu chưa vào trận
        matchmakingQueue.remove(session);

        // Xóa khỏi trận nếu đang chơi
        battleSessions.forEach((battleId, sessions) -> sessions.remove(session));
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

    private String extractQueryParam(String query, String key) {
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }
}