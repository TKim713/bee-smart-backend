package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.SubjectRequest;
import com.api.bee_smart_backend.helper.response.SubjectResponse;
import com.api.bee_smart_backend.model.Subject;
import com.api.bee_smart_backend.repository.SubjectRepository;
import com.api.bee_smart_backend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {
    @Autowired
    private final SubjectRepository subjectRepository;
    private final Instant now = Instant.now();
    private final MapData mapData;

    @Override
    public Map<String, Object> getAllSubjects(String page, String size, String search) {
        int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
        int pageSize = (size != null && !size.isBlank()) ? Integer.parseInt(size) : 10;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Subject> subjectPage;

        if (search == null || search.isBlank()) {
            subjectPage = subjectRepository.findAllByDeletedAtIsNull(pageable);
        } else {
            subjectPage = subjectRepository.findBySubjectNameContainingIgnoreCaseAndDeletedAtIsNull(search, pageable);
        }

        List<SubjectResponse> subjectResponses = subjectPage.getContent().stream()
                .map(subject -> mapData.mapOne(subject, SubjectResponse.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalItems", subjectPage.getTotalElements());
        response.put("totalPages", subjectPage.getTotalPages());
        response.put("currentPage", subjectPage.getNumber());
        response.put("subjects", subjectResponses);

        return response;
    }

    @Override
    public SubjectResponse createSubject(SubjectRequest request) {
        subjectRepository.findBySubjectNameAndDeletedAtIsNull(request.getSubjectName()).ifPresent(existingSubject -> {
            throw new CustomException("Môn học với tên '" + request.getSubjectName() + "' đã tồn tại", HttpStatus.CONFLICT);
        });

        Subject subject = Subject.builder()
                .subjectName(request.getSubjectName())
                .createdAt(now)
                .build();

        subjectRepository.save(subject);
        return mapData.mapOne(subject, SubjectResponse.class);
    }

    @Override
    public SubjectResponse updateSubject(String id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() ->
                new CustomException("Môn học không tồn tại", HttpStatus.NOT_FOUND));
        subject.setSubjectName(request.getSubjectName());
        subject.setUpdatedAt(now);
        subjectRepository.save(subject);
        return mapData.mapOne(subject, SubjectResponse.class);
    }

    @Override
    public void deleteSubjectByIds(List<String> subjectIds) {
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.isEmpty()) {
            throw new CustomException("Không tìm thấy môn học để xóa", HttpStatus.NOT_FOUND);
        }
        subjects.forEach(subject -> subject.setDeletedAt(now));
        subjectRepository.saveAll(subjects);
    }

    @Override
    public SubjectResponse getSubjectBySubjectId(String subjectId) {
        Subject subject = subjectRepository.findBySubjectIdAndDeletedAtIsNull(subjectId)
                .orElseThrow(() -> new CustomException("Không tìm thấy môn học với ID: " + subjectId, HttpStatus.NOT_FOUND));
        return mapData.mapOne(subject, SubjectResponse.class);
    }
}
