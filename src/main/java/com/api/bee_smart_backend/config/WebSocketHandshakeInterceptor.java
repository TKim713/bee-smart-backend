package com.api.bee_smart_backend.config;

import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

            // Use path-based detection instead of instanceof checks
            String path = request.getURI().getPath();
            String token = null;
            String handlerType = "unknown";

            log.info("WebSocket connection attempt to path: {}", path);

            if (path.contains("/ws/battle")) {
                token = httpServletRequest.getParameter("battle-token");
                handlerType = "Battle";
                log.debug("Battle WebSocket connection attempt with token: {}", token != null ? "present" : "missing");
            } else if (path.contains("/ws/notifications")) {
                token = httpServletRequest.getParameter("noti-token");
                handlerType = "Notification";
                log.debug("Notification WebSocket connection attempt with token: {}", token != null ? "present" : "missing");
            } else {
                log.warn("Unknown WebSocket path: {}", path);
                return false;
            }

            if (token == null) {
                log.warn("No token provided for {} WebSocket connection", handlerType);
                return false;
            }

            // Validate token
            if (!jwtTokenUtil.validateToken(token)) {
                log.warn("Invalid token provided for {} WebSocket connection", handlerType);
                return false;
            }

            try {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                log.debug("Token validated for username: {}", username);

                // Get userId from username
                Optional<User> userOptional = userRepository.findByUsernameAndDeletedAtIsNull(username);
                if (userOptional.isEmpty()) {
                    log.warn("User not found or deleted for username: {} in {} WebSocket connection", username, handlerType);
                    return false;
                }

                User user = userOptional.get();
                String userId = user.getUserId();
                attributes.put("userId", userId);
                log.info("User {} successfully authenticated for {} WebSocket connection", userId, handlerType);

                // Extract additional parameters for battle WebSocket
                if (path.contains("/ws/battle")) {
                    String battleId = httpServletRequest.getParameter("battleId");
                    if (battleId != null) {
                        attributes.put("battleId", battleId);
                        log.debug("Battle ID set: {}", battleId);
                    }

                    String gradeId = httpServletRequest.getParameter("gradeId");
                    if (gradeId != null) {
                        attributes.put("gradeId", gradeId);
                        log.debug("Grade ID set: {}", gradeId);
                    }

                    String subjectId = httpServletRequest.getParameter("subjectId");
                    if (subjectId != null) {
                        attributes.put("subjectId", subjectId);
                        log.debug("Subject ID set: {}", subjectId);
                    }
                }

                return true;

            } catch (Exception e) {
                log.error("Error processing {} WebSocket authentication", handlerType, e);
                return false;
            }
        }

        log.warn("Request is not a ServletServerHttpRequest");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }
}