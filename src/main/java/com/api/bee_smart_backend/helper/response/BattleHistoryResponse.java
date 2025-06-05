package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleHistoryResponse {
    private String battleId;
    private String opponentUsername;
    private String gradeName;
    private String subjectName;
    private String topic;
    private boolean won;
    private int score;
    private int correctAnswers;
    private int incorrectAnswers;
    private Instant completedAt;
}
