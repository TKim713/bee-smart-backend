package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.StatisticResponse;

import java.util.Map;

public interface StatisticService {
    StatisticResponse getAggregatedStatisticByUserAndDateRange(String userId, String startDate, String endDate);

    Map<String, Map<String, Integer>> getViewLessonByMonth(String date);

    Map<String, Object> getListQuizRecord(String page, String size, String search);

    Map<String, Object> getListQuizRecordByUser(String userId, String page, String size, String search);

    Map<String, Double> getQuizStatistics();

    Map<String, Map<String, Integer>> getQuizByMonth(String date);

    Map<String, Map<String, Double>> getQuizAverageByMonth(String date);
}
