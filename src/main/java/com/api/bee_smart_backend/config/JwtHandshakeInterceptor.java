package com.api.bee_smart_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        // Support both token parameter names
        String token = queryParams.getFirst("battle-token");
        if (token == null) {
            token = queryParams.getFirst("noti-token");
        }

        if (token != null && jwtTokenUtil.validateToken(token)) {
            String userId = jwtTokenUtil.getUserIdFromToken(token);
            attributes.put("userId", userId);

            // Extract additional parameters for battle if needed
            if (wsHandler instanceof BattleWebSocketHandler) {
                String battleId = queryParams.getFirst("battleId");
                if (battleId != null) {
                    attributes.put("battleId", battleId);
                }

                String gradeId = queryParams.getFirst("gradeId");
                if (gradeId != null) {
                    attributes.put("gradeId", gradeId);
                }

                String subjectId = queryParams.getFirst("subjectId");
                if (subjectId != null) {
                    attributes.put("subjectId", subjectId);
                }
            }

            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // No-op
    }
}