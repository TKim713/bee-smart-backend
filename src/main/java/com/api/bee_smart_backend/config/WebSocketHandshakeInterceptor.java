package com.api.bee_smart_backend.config;

import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

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
            String token = httpServletRequest.getParameter("token");

            if (token != null && jwtTokenUtil.validateToken(token)) {
                String username = jwtTokenUtil.getUsernameFromToken(token);

                // Get userId from username
                Optional<User> userOptional = userRepository.findByUsernameAndDeletedAtIsNull(username);
                if (userOptional.isPresent()) {
                    String userId = userOptional.get().getUserId();
                    attributes.put("userId", userId);

                    // Extract additional parameters
                    String battleId = httpServletRequest.getParameter("battleId");
                    if (battleId != null) {
                        attributes.put("battleId", battleId);
                    }

                    String gradeId = httpServletRequest.getParameter("gradeId");
                    if (gradeId != null) {
                        attributes.put("gradeId", gradeId);
                    }

                    String subjectId = httpServletRequest.getParameter("subjectId");
                    if (subjectId != null) {
                        attributes.put("subjectId", subjectId);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No operation needed
    }
}

