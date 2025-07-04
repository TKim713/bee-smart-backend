package com.api.bee_smart_backend.model;

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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz")
public class Quiz {
    @Id
    private String quizId;
    private String title;
    private String description;
    private String image;
    private int quizDuration;

    @DBRef(lazy = true)
    private Lesson lesson;
    @DBRef(lazy = true)
    private Topic topic;

    @DBRef(lazy = true)
    private List<Question> questions = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;

    public void addQuestion(Question question) {
        questions.add(question);
    }
}

