package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TopicResponse {
    private String topicId;
    private String topicName;
    private int chapter;
    private String gradeName;
    private String semester;
    private List<LessonResponse> lessons;
}
