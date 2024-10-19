package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.model.Grade;
import com.api.bee_smart_backend.repository.GradeRepository;
import com.api.bee_smart_backend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {
    @Autowired
    private GradeRepository gradeRepository;

    @Override
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }
}
