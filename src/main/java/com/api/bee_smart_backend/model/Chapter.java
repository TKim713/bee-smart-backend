package com.api.bee_smart_backend.model;

import lombok.*;
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
@Document(collection = "chapter")
public class Chapter {
    @Id
    private String chapterId;

    private String chapterName;

    @DBRef
    private Grade grade;

    @DBRef
    @ToString.Exclude
    private List<Topic> topics = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;

    public void addTopic(Topic topic) {
        topics.add(topic);
        topic.setChapter(this);
    }
}