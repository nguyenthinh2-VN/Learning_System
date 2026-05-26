package com.example.learning_system_spring.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.jsonwebtoken.Claims;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker cho /queue và /topic
        registry.enableSimpleBroker("/queue", "/topic");
        // Prefix cho message gửi từ client lên server (nếu cần)
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho user-specific destination
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // FE cấu hình CORS riêng khi production
                .withSockJS();                   // SockJS fallback cho browser cũ
    }

    /**
     * Interceptor xác thực JWT khi client gửi CONNECT frame.
     * FE gửi: connectHeaders: { Authorization: "Bearer <token>" }
     * BE parse token → set Principal → Spring dùng để route /user/queue/wallet
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtService.isTokenValid(token)) {
                            Claims claims = jwtService.parseToken(token);
                            String username = claims.getSubject();
                            String role = claims.get("role", String.class);

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            username, null,
                                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }
}
