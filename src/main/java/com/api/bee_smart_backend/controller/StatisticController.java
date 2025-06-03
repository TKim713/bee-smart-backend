package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.service.QuizService;
import com.api.bee_smart_backend.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {
    @Autowired
    private StatisticService statisticService;
    @Autowired
    private QuizService quizService;

    @GetMapping("/user/{userId}/aggregate")
    public ResponseEntity<ResponseObject<StatisticResponse>> getAggregatedStatisticsByDateRange(
            @PathVariable("userId") String userId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            StatisticResponse response = statisticService.getAggregatedStatisticByUserAndDateRange(userId, startDate, endDate);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu thống kê thành công", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu thống kê: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/quiz-records")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getListQuizRecord(
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "subject", required = false) String subject) {
        try {
            Map<String, Object> result = statisticService.getListQuizRecord(page, size, search, subject);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy danh sách lịch sử bài kiểm tra thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/{userId}/quiz-records")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getListQuizRecordByUser(
            @PathVariable String userId,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "subject", required = false) String subject) {
        try {
            Map<String, Object> result = statisticService.getListQuizRecordByUser(userId, page, size, search, subject);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy danh sách lịch sử bài kiểm tra thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/record-lesson-by-month")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Integer>>>> getViewLessonByMonth(
            @RequestParam(required = false) String date,
            @RequestParam(name = "subject", required = false) String subject
    ) {
        try {
            Map<String, Map<String, Integer>> chartData = statisticService.getViewLessonByMonth(date, subject);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu biểu đồ thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu biểu đồ: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/quiz-submit-statistics")
    public ResponseEntity<ResponseObject<Map<String, Double>>> getQuizStatistics(
            @RequestParam(name = "subject", required = false) String subject) {
        try {
            Map<String, Double> chartData = statisticService.getQuizStatistics(subject);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu biểu đồ thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu biểu đồ: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/quiz-by-month")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Integer>>>> getQuizByMonth(
            @RequestParam(required = false) String date,
            @RequestParam(name = "subject", required = false) String subject
    ) {
        try {
            Map<String, Map<String, Integer>> chartData = statisticService.getQuizByMonth(date, subject);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu biểu đồ thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu biểu đồ: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/quiz-average-by-month")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Double>>>> getQuizAverageByMonth(
            @RequestParam(required = false) String date,
            @RequestParam(name = "subject", required = false) String subject
    ) {
        try {
            Map<String, Map<String, Double>> chartData = statisticService.getQuizAverageByMonth(date, subject);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu biểu đồ điểm trung bình thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu biểu đồ: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/quiz-score-by-subject")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Integer>>>> getQuizScoreStatisticsBySubject() {
        try {
            Map<String, Map<String, Integer>> chartData = statisticService.getQuizScoreStatisticsBySubject();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu thống kê điểm theo môn học thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu thống kê: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/battle-users-by-subject")
    public ResponseEntity<ResponseObject<Map<String, Integer>>> getUsersJoinedBattleBySubject() {
        try {
            Map<String, Integer> chartData = statisticService.getUsersJoinedBattleBySubject();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu số người tham gia battle theo môn học thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/battle-score-by-subject")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Integer>>>> getBattleScoreDistributionBySubject() {
        try {
            Map<String, Map<String, Integer>> chartData = statisticService.getBattleScoreDistributionBySubject();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu phân bố điểm battle theo môn học thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/battle-average-by-month")
    public ResponseEntity<ResponseObject<Map<String, Double>>> getBattleAverageByMonth(
            @RequestParam(required = false) String date,
            @RequestParam(name = "subject", required = false) String subject) {
        try {
            Map<String, Double> chartData = statisticService.getBattleAveragePointsByMonth(date, subject);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu điểm trung bình battle theo tháng thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu: " + e.getMessage(), null));
        }
    }
}
