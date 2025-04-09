package com.api.bee_smart_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final BattleWebSocketHandler battleWebSocketHandler;

    public WebSocketConfig(BattleWebSocketHandler battleWebSocketHandler) {
        this.battleWebSocketHandler = battleWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(battleWebSocketHandler, "/ws/game").setAllowedOrigins("*");
    }
}
