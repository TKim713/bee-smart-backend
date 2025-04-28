package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleRequest {
    private String topic;
    private List<String> playerIds;
    // Added fields for grade/subject matchmaking
    private String gradeId;
    private String subjectId;
}
