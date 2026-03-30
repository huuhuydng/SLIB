package slib.com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ObjectProvider<WebSocketAuthChannelInterceptor> webSocketAuthChannelInterceptorProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix cho messages từ server -> client
        config.enableSimpleBroker("/topic");
        // Prefix cho messages từ client -> server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        WebSocketAuthChannelInterceptor interceptor = webSocketAuthChannelInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registration.interceptors(interceptor);
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket - gioi han origin ve cac domain duoc phep
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://slibsystem.site",
                        "https://api.slibsystem.site")
                .withSockJS();

        // Endpoint cho native WebSocket (mobile)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://slibsystem.site",
                        "https://api.slibsystem.site");
    }
}
