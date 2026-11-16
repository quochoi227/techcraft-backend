package com.techcraft.techcraftbackend.config;

import com.techcraft.techcraftbackend.security.CustomUserDetails;
import com.techcraft.techcraftbackend.security.CustomUserDetailsService;
import com.techcraft.techcraftbackend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Hỗ trợ cả SockJS và Native WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                // 1. Xác thực khi CONNECT
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (!StringUtils.hasText(authHeader)) {
                        authHeader = accessor.getFirstNativeHeader("token");
                    }

                    if (StringUtils.hasText(authHeader)) {
                        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
                        try {
                            if (jwtUtils.validateToken(token)) {
                                String email = jwtUtils.extractEmail(token);
                                if (StringUtils.hasText(email)) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                                        UsernamePasswordAuthenticationToken authentication =
                                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                        accessor.setUser(authentication);
                                        log.info("WebSocket authenticated user: {}", email);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("WebSocket authentication failed: {}", e.getMessage());
                        }
                    }
                }

                // 2. Phân quyền kênh khi SUBSCRIBE
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null) {
                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
                        if (auth == null || !auth.isAuthenticated()) {
                            log.warn("Unauthorized subscription attempt to destination: {}", destination);
                            throw new AccessDeniedException("Vui lòng đăng nhập để đăng ký nhận thông báo.");
                        }

                        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                        boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                        // Kênh Admin: chỉ ROLE_ADMIN mới được subscribe
                        if (destination.startsWith("/topic/admin")) {
                            if (!isAdmin) {
                                log.warn("User {} denied access to admin channel: {}", userDetails.getUsername(), destination);
                                throw new AccessDeniedException("Chỉ quản trị viên mới có quyền đăng ký kênh này.");
                            }
                        }

                        // Kênh User: chỉ chính user đó hoặc admin mới được subscribe
                        if (destination.startsWith("/topic/users/")) {
                            String[] parts = destination.split("/");
                            // Destination dạng: /topic/users/{userId}/...
                            if (parts.length >= 4) {
                                String targetUserId = parts[3];
                                if (!isAdmin && !userDetails.getId().toString().equalsIgnoreCase(targetUserId)) {
                                    log.warn("User {} denied access to user channel: {}", userDetails.getUsername(), destination);
                                    throw new AccessDeniedException("Bạn không có quyền đăng ký kênh thông báo của người khác.");
                                }
                            }
                        }
                    }
                }

                return message;
            }
        });
    }
}
