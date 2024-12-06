package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StatisticResponse {
    private int numberOfQuestionsAnswered;
    private long timeSpentLearning;
    private int numberOfQuizzesDone;
    private long timeSpentDoingQuizzes;
    private LocalDate startDate;
    private LocalDate endDate;
}
