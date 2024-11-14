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
@Table(name="grade")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long grade_id;

    @Column(nullable = false)
    private String grade_name;

    @OneToMany(mappedBy = "grade")
    private List<Chapter> chapters;

    @Column(nullable = false)
    private Timestamp create_at;
    private Timestamp update_at;
    private Timestamp delete_at;

    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
        chapter.setGrade(this);
    }
}
