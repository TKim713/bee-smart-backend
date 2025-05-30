package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleWebSocketMessage {
    private String type; // "answer", "quit", etc.
    private String battleId;
    private String userId;
    private String questionId;
    private String answer;
    private List<String> answers;
    private int timeTaken;
}