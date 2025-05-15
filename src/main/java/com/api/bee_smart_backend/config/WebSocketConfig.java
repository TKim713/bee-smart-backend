package com.api.bee_smart_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Autowired
    private BattleWebSocketHandler battleWebSocketHandler;

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(battleWebSocketHandler, "/ws/battle")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*"); // Consider restricting in production
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
