package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.AnswerRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.model.Battle;

import java.util.List;
import java.util.Map;

public interface BattleService {
    // 📌 Thêm người chơi vào hàng đợi
    BattleResponse matchPlayers(BattleRequest request);

    Map<String, Object> getAllBattles(String page, String size, String search);

    // 📌 Tạo một trận đấu mới
    BattleResponse createBattle(BattleRequest request);

    // 📌 Gửi câu trả lời
    BattleResponse submitAnswer(String battleId, AnswerRequest request);

    // 📌 Lấy chi tiết một trận đấu
    BattleResponse getBattleById(String battleId);

    // 📌 Kết thúc trận đấu
    void endBattle(String battleId);

    String checkMatchmakingStatus(String topic);
}
