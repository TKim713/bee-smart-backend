package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserWithBattleStatsResponse {
    private String userId;
    private String username;
    private int totalBattleWon;
    private int totalBattleLost;
}
