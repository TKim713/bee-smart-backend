package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TopicResponse {
    private String topicId;
    private String topicName;
    private int topicNumber;
    private String gradeName;
    private String subjectName;
    private String semester;
    private String bookName;
}
