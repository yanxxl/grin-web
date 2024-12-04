package ws

import grin.web.HttpSessionConfigurator
import groovy.util.logging.Slf4j

import jakarta.servlet.http.HttpSession
import jakarta.websocket.*
import jakarta.websocket.server.HandshakeRequest
import jakarta.websocket.server.ServerEndpoint
import jakarta.websocket.server.ServerEndpointConfig

/**
 * ws.WebSocketEntry
 * 入口，也作示例
 * 如果这个不适合，可以在项目初始化时，替换掉。
 * 客户端测试代码：let ws = new WebSocket('ws://localhost:8080/ws'); ws.send('hi')
 */
@Slf4j
@ServerEndpoint(value = "/ws", configurator = HttpSessionConfigurator)
class WebSocketEntry {
    @OnOpen
    void onOpen(Session session, EndpointConfig config) {
        HandshakeRequest request = config.getUserProperties().get(HandshakeRequest.name)
        session.getUserProperties().put(HttpSession.name, request.getHttpSession())
        log.info("Connected ${session.id} from ${request.getHttpSession().id}")
    }

    @OnMessage
    String onMessage(String message, Session session) {
        HttpSession httpSession = session.getUserProperties().get(HttpSession.name)
        log.info("收到 ${message} and count ${httpSession.getAttribute('count')}")
        return "ok"
    }

    @OnClose
    void onClose(Session session, CloseReason closeReason) {
        log.info(String.format("Session %s closed because of %s", session.getId(), closeReason))
    }
}

/**
 * 为 WebSocket 提供 handshake request，方便获取信息。
 */
class HttpSessionConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        sec.getUserProperties().put(HandshakeRequest.name, request)
    }
}