package com.api.bee_smart_backend.helper.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LessonResponse {
    private long lesson_id;
    private String lesson_name;
    private String description;
    private String content;
}
