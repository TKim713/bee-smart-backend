package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.StatisticResponse;

import java.util.Map;

public interface StatisticService {
    StatisticResponse getAggregatedStatisticByUserAndDateRange(String userId, String startDate, String endDate);

    Map<String, Map<String, Integer>> getViewLessonByMonth(String date, String subject);

    Map<String, Object> getListQuizRecord(String page, String size, String search, String subject);

    Map<String, Object> getListQuizRecordByUser(String userId, String page, String size, String search, String subject);

    Map<String, Double> getQuizStatistics(String subject);

    Map<String, Map<String, Integer>> getQuizByMonth(String date, String subject);

    Map<String, Map<String, Double>> getQuizAverageByMonth(String date, String subject);

    Map<String, Map<String, Integer>> getQuizScoreStatisticsBySubject();

    Map<String, Integer> getUsersJoinedBattleBySubject();

    Map<String, Map<String, Integer>> getBattleScoreDistributionBySubject();

    Map<String, Double> getBattleAveragePointsByMonth(String date, String subjectName);
}
