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
public class BattleUserResponse {
    private String battleUserId;
    private String userId;
    private String username;
    private int totalBattleWon;
    private int totalBattleLost;
    private List<BattleHistoryResponse> historyResponses;
}
