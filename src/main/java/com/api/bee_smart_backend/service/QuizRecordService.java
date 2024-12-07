package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.QuizCountByGradeResponse;
import com.api.bee_smart_backend.model.QuizRecord;

import java.util.List;

public interface QuizRecordService {
    List<QuizRecord> getQuizRecords(String userId);

}
