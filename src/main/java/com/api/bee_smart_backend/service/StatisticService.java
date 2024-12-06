package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.StatisticResponse;

import java.time.LocalDate;
import java.util.List;

public interface StatisticService {
    StatisticResponse getAggregatedStatisticByUserAndDateRange(String jwtToken, String startDate, String endDate);
}
