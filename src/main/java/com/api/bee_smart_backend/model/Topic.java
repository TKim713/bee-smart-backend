package com.api.bee_smart_backend.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "topic")
public class Topic {
    @Id
    private String topicId;
    private String topicName;
    private int topicNumber;

    @DBRef
    private Grade grade;
    @DBRef
    private Subject subject;
    private String semester;

    @DBRef
    private BookType bookType;

    @DBRef
    @ToString.Exclude
    private List<Lesson> lessons = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setTopic(this);
    }
}
