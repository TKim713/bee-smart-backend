package com.api.bee_smart_backend.model;

import com.api.bee_smart_backend.helper.enums.QuestionType;
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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "question")
public class Question {
    @Id
    private String questionId;
    private String content;
    private String image;
    private List<String> options;

    private QuestionType questionType;

    private String correctAnswer;
    private List<String> correctAnswers;
    private List<String> answers;

    @DBRef
    private Quiz quiz;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedDate
    private Instant deletedAt;
}

