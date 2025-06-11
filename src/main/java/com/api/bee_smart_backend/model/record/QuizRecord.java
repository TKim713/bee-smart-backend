package com.api.bee_smart_backend.model.record;

import com.api.bee_smart_backend.model.Quiz;
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
@Document(collection = "quiz_record")
public class QuizRecord {
    @Id
    private String recordId;

    @DBRef(lazy = true)
    private User user;

    @DBRef(lazy = true)
    private Quiz quiz;
    private String gradeName;

    private int totalQuestions;
    private int correctAnswers;
    private double points;
    private long timeSpent;

    private LocalDate submitDate;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;
}
