package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.GradeRequest;
import com.api.bee_smart_backend.helper.response.GradeResponse;
import com.api.bee_smart_backend.model.Grade;

import java.util.List;
import java.util.Map;

public interface GradeService {

    Map<String, Object> getAllGrades(String page, String size, String search);

    Grade createGrade(GradeRequest request);

    GradeResponse updateGradeById(String gradeId, GradeRequest request);

    void deleteGradeByIds(List<String> gradeIds);

}
