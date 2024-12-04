package ws

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpSession
import jakarta.websocket.*
import jakarta.websocket.server.HandshakeRequest
import jakarta.websocket.server.ServerEndpoint
import jakarta.websocket.server.ServerEndpointConfig

/**
 * WebSocketEntry 示例
 * 客户端测试代码：let ws = new WebSocket('ws://localhost:8080/ws'); ws.send('hi')
 */
@Slf4j
@ServerEndpoint(value = "/ws", configurator = SessionConfigurator)
class WebSocketEntry {
    @OnOpen
    void onOpen(Session session, EndpointConfig config) {
        String userId = config.getUserProperties().get("userId")
        session.getUserProperties().put("userId", userId)
        log.info("Open ${session.id}, by ${userId}")
    }

    @OnMessage
    String onMessage(String message, Session session) {
        String userId = session.getUserProperties().get("userId")
        log.info("Message ${message} and userId ${userId}")
        return "ok"
    }

    @OnClose
    void onClose(Session session, CloseReason closeReason) {
        log.info("Close ${session.id} ${closeReason}")
    }

    @OnError
    void onError(Session session, Throwable thr) {
        log.warn("Error ${session.id} ${thr.getMessage()}")
    }
}

/**
 * 为 WebSocket 提供 handshake request，方便获取信息。
 */
@Slf4j
class SessionConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 传递用户信息，以前（undertow）这里可以直接把 request 直接传递过去，现在（Jetty）常有 null，大概用过就回收了。
        // 此处获取 session 信息是可以的，可以把用户的信息传递过去。count 是 hello 页面存到 session 里的值。此处不是 catalog 作用域，要用原始 API。
        HttpSession httpSession = request.getHttpSession()
        log.info("Handshake from http session ${httpSession?.id},current count ${httpSession?.getAttribute('count') ?: 0}")
        sec.getUserProperties().put("userId", "111")
    }
}