package slib.com.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.entity.users.User;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.security.JwtService;
import slib.com.example.security.KioskDevicePrincipal;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final KioskTokenService kioskTokenService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            return message;
        }

        Authentication authentication = extractAuthentication(accessor);
        if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor.getDestination(), authentication);
        } else if (StompCommand.SEND.equals(command)) {
            validateSend(accessor.getDestination(), authentication);
        }

        return message;
    }

    private Authentication authenticate(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Thiếu Authorization header cho WebSocket");
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new AccessDeniedException("Token WebSocket không hợp lệ");
        }

        if (kioskTokenService.isKioskDeviceToken(token)) {
            KioskConfigEntity kiosk = kioskTokenService.validateDeviceToken(token);
            if (kiosk == null) {
                throw new AccessDeniedException("Kiosk device token không hợp lệ hoặc đã hết hạn");
            }
            return new KioskDevicePrincipal(kiosk.getId(), kiosk.getKioskCode());
        }

        String username = jwtService.extractUsername(token);
        if (username == null || !jwtService.isAccessToken(token)) {
            throw new AccessDeniedException("Access token WebSocket không hợp lệ");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
            throw new AccessDeniedException("Access token WebSocket đã hết hạn hoặc không hợp lệ");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Authentication extractAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }
        throw new AccessDeniedException("Kết nối WebSocket chưa được xác thực");
    }

    private void validateSubscription(String destination, Authentication authentication) {
        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Destination WebSocket không hợp lệ");
        }

        if (destination.startsWith("/topic/chat/seen/")) {
            requireUserMatchesTopic(authentication, destination.substring("/topic/chat/seen/".length()));
            return;
        }

        if (destination.startsWith("/topic/chat/")) {
            requireUserMatchesTopic(authentication, destination.substring("/topic/chat/".length()));
            return;
        }

        if (destination.startsWith("/topic/notifications/")) {
            requireUserMatchesTopic(authentication, destination.substring("/topic/notifications/".length()));
            return;
        }

        if (destination.startsWith("/topic/conversation/")) {
            UUID conversationId = parseUuid(destination.substring("/topic/conversation/".length()));
            verifyConversationAccess(conversationId, requireUserId(authentication));
            return;
        }

        if ("/topic/escalate".equals(destination) || "/topic/librarian-notifications".equals(destination)) {
            requireAnyRole(authentication, "ROLE_ADMIN", "ROLE_LIBRARIAN");
            return;
        }

        if (destination.startsWith("/topic/kiosk/") && destination.endsWith("/session-updated")) {
            requireKioskAccess(authentication, destination);
            return;
        }

        if ("/topic/access-logs".equals(destination)) {
            requireAnyRole(authentication, "ROLE_ADMIN", "ROLE_LIBRARIAN", "ROLE_KIOSK");
            return;
        }

        if ("/topic/dashboard".equals(destination)
                || "/topic/seats".equals(destination)
                || "/topic/news".equals(destination)
                || "/topic/library/entries".equals(destination)) {
            return;
        }
    }

    private void validateSend(String destination, Authentication authentication) {
        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Destination gửi WebSocket không hợp lệ");
        }

        if ("/app/chat".equals(destination)) {
            requireUserId(authentication);
            return;
        }

        if (destination.startsWith("/app/typing/")) {
            UUID conversationId = parseUuid(destination.substring("/app/typing/".length()));
            verifyConversationAccess(conversationId, requireUserId(authentication));
        }
    }

    private void verifyConversationAccess(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AccessDeniedException("Conversation không tồn tại"));

        boolean isStudent = conversation.getStudent() != null && conversation.getStudent().getId().equals(userId);
        boolean isAssignedLibrarian = conversation.getLibrarian() != null
                && conversation.getLibrarian().getId().equals(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy user cho kết nối WebSocket"));
        boolean isLibrarianRole = user.getRole() == slib.com.example.entity.users.Role.LIBRARIAN
                || user.getRole() == slib.com.example.entity.users.Role.ADMIN;

        if (!isStudent && !isAssignedLibrarian && !isLibrarianRole) {
            throw new AccessDeniedException("Bạn không có quyền truy cập cuộc hội thoại này");
        }
    }

    private void requireUserMatchesTopic(Authentication authentication, String userIdSegment) {
        UUID currentUserId = requireUserId(authentication);
        UUID destinationUserId = parseUuid(userIdSegment);
        if (!Objects.equals(currentUserId, destinationUserId)) {
            throw new AccessDeniedException("Bạn không có quyền subscribe topic riêng tư này");
        }
    }

    private void requireKioskAccess(Authentication authentication, String destination) {
        if (hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_LIBRARIAN")) {
            return;
        }

        if (!(authentication instanceof KioskDevicePrincipal kioskPrincipal)) {
            throw new AccessDeniedException("Topic kiosk yêu cầu kiosk device token hợp lệ");
        }

        String prefix = "/topic/kiosk/";
        String suffix = "/session-updated";
        String kioskCode = destination.substring(prefix.length(), destination.length() - suffix.length());
        if (!kioskPrincipal.getKioskCode().equals(kioskCode)) {
            throw new AccessDeniedException("Kiosk không có quyền subscribe topic của thiết bị khác");
        }
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication instanceof KioskDevicePrincipal) {
            throw new AccessDeniedException("Kiosk device không có quyền truy cập topic người dùng");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy user cho kết nối WebSocket"));
        return user.getId();
    }

    private void requireAnyRole(Authentication authentication, String... roles) {
        if (!hasAnyRole(authentication, roles)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập topic này");
        }
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (role.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.warn("Topic WebSocket không hợp lệ: {}", value);
            throw new AccessDeniedException("Destination WebSocket không hợp lệ");
        }
    }
}
