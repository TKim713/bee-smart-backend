package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.ChapterRequest;
import com.api.bee_smart_backend.helper.response.ChapterResponse;

import java.util.Map;

public interface ChapterService {

    ChapterResponse createChapterByGradeId(String gradeId, ChapterRequest request);

    Map<String, Object> getListChapterByGrade(String gradeId, int limit, int skip);
}
