package com.autowashpro.autowashpro_be.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Xác thực JWT khi client mở kết nối STOMP (CONNECT).
 * Token truyền qua native header "Authorization: Bearer ..." hoặc "token".
 * Gán Principal name = "{STAFF|CUSTOMER}:{id}" để định tuyến /user.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = resolveToken(accessor);
        if (token != null && jwtTokenProvider.isTokenValid(token)) {
            UserPrincipal.UserType userType = jwtTokenProvider.extractUserType(token);
            Long id = userType == UserPrincipal.UserType.STAFF
                    ? jwtTokenProvider.extractStaffId(token)
                    : jwtTokenProvider.extractCustomerId(token);
            if (id != null) {
                accessor.setUser(new WsPrincipal(userType.name() + ":" + id));
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearer = accessor.getFirstNativeHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return accessor.getFirstNativeHeader("token");
    }
}
