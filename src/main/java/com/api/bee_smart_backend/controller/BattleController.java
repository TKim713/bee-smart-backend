package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleHistoryResponse;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.BattleUserResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.service.BattleService;
import com.api.bee_smart_backend.service.GradeService;
import com.api.bee_smart_backend.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/battles")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private TokenRepository tokenRepository;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getAllBattles(
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = battleService.getAllBattles(page, size, search);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Battle list retrieved successfully", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error retrieving battle list: " + e.getMessage(), null));
        }
    }

    @PostMapping("/matchmaking/start")
    public ResponseEntity<ResponseObject<BattleResponse>> matchPlayer(@RequestBody BattleRequest request) {
        try {
            BattleResponse battleResponse = battleService.matchPlayers(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Match created successfully!", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error creating match: " + e.getMessage(), null));
        }
    }

    @GetMapping("/matchmaking/status")
    public ResponseEntity<ResponseObject<String>> getMatchmakingStatus(
            @RequestParam String gradeId,
            @RequestParam String subjectId) {
        try {
            String status = battleService.checkMatchmakingStatus(gradeId, subjectId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Matchmaking status retrieved", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error getting matchmaking status: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{battleId}/answer")
    public ResponseEntity<ResponseObject<BattleResponse>> submitAnswer(
            @PathVariable String battleId,
            @RequestBody AnswerRequest request) {
        try {
            BattleResponse battleResponse = battleService.submitAnswer(battleId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Answer submitted successfully!", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error submitting answer: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{battleId}/end")
    public ResponseEntity<ResponseObject<String>> endBattle(@PathVariable String battleId) {
        try {
            battleService.endBattle(battleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Battle ended successfully!", "Battle " + battleId + " ended successfully"));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error ending battle: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{battleId}")
    public ResponseEntity<ResponseObject<BattleResponse>> getBattleById(@PathVariable String battleId) {
        try {
            BattleResponse battleResponse = battleService.getBattleById(battleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Battle details retrieved successfully", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error retrieving battle details: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{battleId}/nextQuestion")
    public ResponseEntity<ResponseObject<BattleResponse>> sendNextQuestion(@PathVariable String battleId) {
        try {
            BattleResponse battleResponse = battleService.sendNextQuestion(battleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Next question sent successfully!", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error sending next question: " + e.getMessage(), null));
        }
    }

    @GetMapping("/get-online-list")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getOnlineList(
            @RequestHeader("Authorization") String token,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false) String search) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Map<String, Object> response = battleService.getOnlineList(jwtToken, page, size, search);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Danh sách người dùng trực tuyến", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/battle-user-detail")
    public ResponseEntity<ResponseObject<BattleUserResponse>> getBattleUserDetail(
            @RequestHeader("Authorization") String token,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            int pageNumber = (page != null && !page.isBlank()) ? Integer.parseInt(page) : 0;
            int pageSize = (size != null && !page.isBlank()) ? Integer.parseInt(size) : 10;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "endTime"));

            BattleUserResponse response = battleService.getBattleUserDetail(jwtToken, pageable);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Thông tin thống kê và lịch sử trận đấu của người dùng", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi lấy thông tin trận đấu", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    // BattleController.java
    @PostMapping("/{battleId}/leave")
    public ResponseEntity<ResponseObject<String>> leaveBattle(
            @PathVariable String battleId,
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Token userToken = tokenRepository.findByAccessToken(jwtToken)
                    .orElseThrow(() -> new CustomException("Invalid token", HttpStatus.UNAUTHORIZED));
            String userId = userToken.getUser().getUserId();

            battleService.endBattleWithWinner(battleId, userId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "You have left the battle!", "Battle " + battleId + " ended due to player leaving"));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error leaving battle: " + e.getMessage(), null));
        }
    }
}