package com.api.bee_smart_backend.helper.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LessonResponse {
    private String lessonId;
    private String lessonName;
    private String gradeName;
    private int lessonNumber;
    private String description;
    private String content;
    private int viewCount;
}
