package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.SubjectRequest;
import com.api.bee_smart_backend.helper.response.SubjectResponse;

import java.util.List;
import java.util.Map;

public interface SubjectService {

    Map<String, Object> getAllSubjects(String page, String size, String search);

    SubjectResponse createSubject(SubjectRequest request);

    SubjectResponse updateSubject(String id, SubjectRequest request);

    void deleteSubjectByIds(List<String> subjectIds);

    SubjectResponse getSubjectBySubjectId(String subjectId);
}
