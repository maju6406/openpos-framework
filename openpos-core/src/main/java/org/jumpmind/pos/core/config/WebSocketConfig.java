package org.jumpmind.pos.core.config;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    final protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    MutableBoolean initialized;

    @Autowired
    Environment env;

    ObjectMapper mapper = new ObjectMapper();

    @Value("${openpos.compatibility.version:v1}") // Default to 'v1' so
                                                  // pre-existing clients aren't
                                                  // broken
    String serverCompatibilityVersion;

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(80000); // 75681
        registration.setSendBufferSizeLimit(80000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic").setTaskScheduler(new DefaultManagedTaskScheduler()).setHeartbeatValue(new long[] { 0, 20000 });
        config.setApplicationDestinationPrefixes("/app");
        config.setPathMatcher(new AntPathMatcher("."));
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        /*
         * https://stackoverflow.com/questions/29689838/sockjs-receive-stomp-
         * messages-from-spring-websocket-out-of-order
         * 
         * Also tried setting the poolSize to 1 on the clientInboundChannel configuration, but then we started to see
         * DeviceResponses sent back from the client getting dropped.  Perhaps the inbound problem on the websocket is due to
         * near-simultaneously transmission of various messages types (Screens, DeviceResponse, KeepAlive) from the client to the 
         * server, making the problem more prevalent?  Something to keep an eye on.
         */
        registration.taskExecutor().corePoolSize(1);
        registration.interceptors(new ChannelInterceptorAdapter() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                Message<?> returnMessage = message;
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
                if (accessor != null) {
                    SimpMessageType messageType = accessor.getMessageType();
                    if (messageType == SimpMessageType.MESSAGE) {
                        // Add compatibility version to message headers going
                        // out to client
                        if (WebSocketConfig.this.serverCompatibilityVersion != null) {
                            accessor.addNativeHeader(MessageUtils.COMPATIBILITY_VERSION_HEADER,
                                    WebSocketConfig.this.serverCompatibilityVersion);
                            returnMessage = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                        }
                    }
                }
                return returnMessage;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api").setAllowedOrigins("*").withSockJS().setInterceptors(new HandshakeInterceptor() {

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception) {
            }

            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) throws Exception {
                if (!initialized.booleanValue()) {
                    logger.info("Rejected websocket communication attempt because the server is not initialized");
                    return false;
                } else {
                    return true;
                }

            }
        });
    }

}