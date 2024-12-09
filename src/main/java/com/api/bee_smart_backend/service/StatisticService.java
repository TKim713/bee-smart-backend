package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.StatisticResponse;

import java.util.Map;

public interface StatisticService {
    StatisticResponse getAggregatedStatisticByUserAndDateRange(String jwtToken, String startDate, String endDate);

    Map<String, Map<String, Integer>> getViewLessonByMonth(String date);
}
