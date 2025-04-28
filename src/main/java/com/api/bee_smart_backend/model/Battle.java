package com.api.bee_smart_backend.model;

import com.api.bee_smart_backend.model.dto.PlayerScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "battle")
public class Battle {
    @Id
    private String id;
    private String topic;
    private String status; // "ONGOING" or "ENDED"
    private List<PlayerScore> playerScores;
    private String winner;

    // Fields for grade and subject-based matchmaking
    private String gradeId;
    private String subjectId;

    private Instant startTime;
    private Instant endTime;
    private Set<String> answeredQuestions = new HashSet<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;
}
