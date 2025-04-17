package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.SubjectRequest;
import com.api.bee_smart_backend.helper.response.SubjectResponse;

import java.util.List;

public interface SubjectService {

    List<SubjectResponse> getAllSubjects();

    SubjectResponse createSubject(SubjectRequest request);

    SubjectResponse updateSubject(String id, SubjectRequest request);

    void deleteSubjectByIds(List<String> subjectIds);
}
