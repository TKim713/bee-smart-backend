package com.api.bee_smart_backend.helper.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LessonResponse {
    private String lesson_name;
    private String description;
    private String content;
    private String grade_name;
    private String topic;
}
