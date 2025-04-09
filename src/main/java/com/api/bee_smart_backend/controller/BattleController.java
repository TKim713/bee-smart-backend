package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.BattleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/battles")
public class BattleController {

    @Autowired
    private BattleService battleService;

    @GetMapping
    public ResponseEntity<ResponseObject<Map<String, Object>>> getAllBattles(
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "search", required = false, defaultValue = "") String search) {
        try {
            Map<String, Object> result = battleService.getAllBattles(page, size, search);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy danh sách trận đấu thành công", result.isEmpty() ? Collections.emptyMap() : result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy danh sách trận đấu: " + e.getMessage(), null));
        }
    }

    @PostMapping("/matchmaking/start")
    public ResponseEntity<ResponseObject<BattleResponse>> matchPlayer(@RequestBody BattleRequest request) {
        try {
            BattleResponse battleResponse = battleService.matchPlayers(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Ghép trận đấu thành công!", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi ghép trận đấu: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{battleId}/answer")
    public ResponseEntity<ResponseObject<BattleResponse>> submitAnswer(
            @PathVariable String battleId,
            @RequestBody AnswerRequest request) {
        try {
            BattleResponse battleResponse = battleService.submitAnswer(battleId, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Câu trả lời được gửi thành công!", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi gửi câu trả lời: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{battleId}/end")
    public ResponseEntity<ResponseObject<String>> endBattle(@PathVariable String battleId) {
        try {
            battleService.endBattle(battleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Trận đấu đã kết thúc!", "Trận đấu " + battleId + " kết thúc thành công"));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi kết thúc trận đấu: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{battleId}")
    public ResponseEntity<ResponseObject<BattleResponse>> getBattleById(@PathVariable String battleId) {
        try {
            BattleResponse battleResponse = battleService.getBattleById(battleId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Lấy thông tin trận đấu thành công", battleResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi lấy thông tin trận đấu: " + e.getMessage(), null));
        }
    }
}