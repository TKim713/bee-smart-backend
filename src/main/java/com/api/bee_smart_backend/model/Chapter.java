package com.api.bee_smart_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long chapter_id;

    @Column(nullable = false)
    private String chapter_name;

    @ManyToOne
    @JoinColumn(name = "grade_id")
    private Grade grade;

    @OneToMany(mappedBy = "chapter")
    private List<Topic> topics;

    @Column(nullable = false)
    private Timestamp create_at;
    private Timestamp update_at;
    private Timestamp delete_at;

    public void addTopic(Topic topic) {
        topics.add(topic);
        topic.setChapter(this);
    }
}
