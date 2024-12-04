import grin.app.GrinApplication
import grin.web.GServlet
import groovy.util.logging.Slf4j
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer
import org.eclipse.jetty.server.Server

/**
 * 启动 APP*/
@Slf4j
class Application {
    /**
     * 启动 Server
     * 添加 GServlet 及 Websockets。
     * 简单启动服务，不支持 Gzip，SSL 等，交由前端 Nginx 去支持。
     */
    void start() {
        GrinApplication app = GrinApplication.init(this)
        String host = 'localhost'
        int port = app.isDev() ? 8080 : 8090
        String context = '/'

        app.initializeDB()
        app.initializeWeb()

        Server server = new Server(port);
        ServletContextHandler servletContextHandler = new ServletContextHandler(context);
        servletContextHandler.setHandler(new SessionHandler())
        servletContextHandler.addServlet(GServlet, '/*')
        JakartaWebSocketServletContainerInitializer.configure(servletContextHandler, (servletContext, container) -> {
            app.websockets.each { container.addEndpoint(it) }
        })
        server.setHandler(servletContextHandler)
        server.start()
        log.info("start server @ http://${host}:${port}${context}")
    }

    /**
     * 启动应用
     * @param args
     */
    static void main(String[] args) {
        new Application().start()
    }
}
