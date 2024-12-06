package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {
    @Autowired
    private StatisticService statisticService;

    @GetMapping("/aggregate")
    public ResponseEntity<ResponseObject<StatisticResponse>> getAggregatedStatisticsByDateRange(
            @RequestHeader("Authorization") String token,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            StatisticResponse response = statisticService.getAggregatedStatisticByUserAndDateRange(jwtToken, startDate, endDate);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Fetched aggregated statistics successfully", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error fetching aggregated statistics: " + e.getMessage(), null));
        }
    }
}
