package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.GradeResponse;
import com.api.bee_smart_backend.helper.response.LessonResponse;

import java.util.List;

public interface GradeService {
    List<GradeResponse> getAllGrades();

    GradeResponse createGrade(GradeRequest request);

    List<LessonResponse> getLessonsByGrade(Long gradeId);
}
