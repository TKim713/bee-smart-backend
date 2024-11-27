package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.GradeResponse;
import com.api.bee_smart_backend.model.Grade;

import java.util.List;
import java.util.Map;

public interface GradeService {
    List<GradeResponse> getAllGrades();

    Grade createGrade(GradeRequest request);

    //Map<String, Object> getLessonsByGrade(String gradeId, int limit, int skip);
}
