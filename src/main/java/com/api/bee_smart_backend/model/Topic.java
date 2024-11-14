package com.api.bee_smart_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="topic")
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long topic_id;

    @Column(nullable = false)
    private String topic_name;

    @ManyToOne
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @OneToMany(mappedBy = "topic")
    private List<Lesson> lessons;

    @Column(nullable = false)
    private Timestamp create_at;
    private Timestamp update_at;
    private Timestamp delete_at;

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setTopic(this);
    }
}
