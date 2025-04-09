package com.api.bee_smart_backend.helper.response;

import com.api.bee_smart_backend.model.dto.PlayerScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleResponse {
    private String battleId;
    private String topic;
    private String status; // "ONGOING", "ENDED"
    private List<PlayerScore> playerScores;
    private String winner;
    // Added fields for grade/subject info
    private String gradeId;
    private String subjectId;
}
