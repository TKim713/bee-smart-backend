package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleWebSocketMessage {
    private String type;        // "join", "answer", "result", etc.
    private String battleId;
    private String questionId;
    private String userId;
    private String answer;
    private int timeTaken;
}
