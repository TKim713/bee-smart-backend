package com.api.bee_smart_backend.service;

import java.util.Map;

public interface LessonService {
    Map<String, Object> getAllLessons(String page, String size, String search);
}
