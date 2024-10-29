package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LessonRequest {
    private String lesson_name;
    private String description;
    private String content;
    private String grade_name;
    private String topic_name;
}
