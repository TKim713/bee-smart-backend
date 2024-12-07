package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.QuizCountByGradeResponse;
import com.api.bee_smart_backend.model.QuizRecord;

import java.util.List;
import java.util.Map;

public interface QuizRecordService {
    Map<String, Object> getListQuizRecord(String page, String size, String search);

    List<QuizRecord> getQuizRecords(String userId);

}
