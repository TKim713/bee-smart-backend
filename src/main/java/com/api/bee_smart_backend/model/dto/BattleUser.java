package com.api.bee_smart_backend.model.dto;

import com.api.bee_smart_backend.model.Battle;
import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.model.Subject;
import com.api.bee_smart_backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "battle_user")
public class BattleUser {
    @Id
    private String battleUserId;

    @DBRef(lazy = true)
    private User user;
    private int totalBattleWon;
    private int totalBattleLost;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;
}