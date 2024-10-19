package com.api.bee_smart_backend.model;

import com.api.bee_smart_backend.helper.response.LessonResponse;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="lesson")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long lesson_id;

    @Column(nullable = false)
    private String lesson_name;

    private String description;

    @Column(nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private Timestamp create_at;
    private Timestamp update_at;
    private Timestamp delete_at;
}
