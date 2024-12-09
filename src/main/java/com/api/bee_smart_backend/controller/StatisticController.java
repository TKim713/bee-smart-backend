package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.QuizCountByGradeResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.StatisticResponse;
import com.api.bee_smart_backend.service.QuizRecordService;
import com.api.bee_smart_backend.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {
    @Autowired
    private StatisticService statisticService;
    @Autowired
    private QuizRecordService quizRecordService;

    @GetMapping("/aggregate")
    public ResponseEntity<ResponseObject<StatisticResponse>> getAggregatedStatisticsByDateRange(
            @RequestHeader("Authorization") String token,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            StatisticResponse response = statisticService.getAggregatedStatisticByUserAndDateRange(jwtToken, startDate, endDate);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu thống kê thành công", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu thống kê: " + e.getMessage(), null));
        }
    }
//    @GetMapping("/quiz-count-by-grade")
//    public ResponseEntity<ResponseObject<List<QuizCountByGradeResponse>>> getQuizCountByGrade() {
//        try {
//            List<QuizCountByGradeResponse> quizCounts = quizRecordService.getQuizCountByGrade();
//
//            return ResponseEntity.status(HttpStatus.OK)
//                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy số lượng bài quiz theo lớp thành công", quizCounts));
//        } catch (CustomException e) {
//            return ResponseEntity.status(e.getStatus())
//                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu: " + e.getMessage(), null));
//        }
//    }

    @GetMapping("/view-lesson-by-month")
    public ResponseEntity<ResponseObject<Map<String, Map<String, Integer>>>> getViewLessonByMonth(
            @RequestParam(required = false) String date
    ) {
        try {
            Map<String, Map<String, Integer>> chartData = statisticService.getViewLessonByMonth(date);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy dữ liệu biểu đồ thành công", chartData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi lấy dữ liệu biểu đồ: " + e.getMessage(), null));
        }
    }
}
