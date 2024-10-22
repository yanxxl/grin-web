import grin.app.GrinApplication
import grin.web.GServlet
import groovy.util.logging.Slf4j
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.encoding.EncodingHandler
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentInfo
import io.undertow.servlet.api.DeploymentManager
import io.undertow.websockets.jsr.WebSocketDeploymentInfo

import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.servlet.MultipartConfigElement
import java.security.KeyStore

/**
 * 启动 APP
 */
@Slf4j
class Application {
    /**
     * 启动 Server
     */
    void start() {
        GrinApplication app = GrinApplication.init(this)

        String host = 'localhost'
        int port = app.isDev() ? 8080 : 8090
        int httpsPort = -1
        String jksPath = ''
        String jksPwd = ''
        String context = '/'
        String uploadLocation = ''
        long maxFileSize = -1
        long maxRequestSize = -1
        int fileSizeThreshold = 0
        int ioThreads = 2
        int workerThreads = 5

        app.initializeDB()
        app.initializeWeb()

        WebSocketDeploymentInfo webSockets = new WebSocketDeploymentInfo()
        app.websockets.each { webSockets.addEndpoint(it) }
        DeploymentInfo deploymentInfo = Servlets.deployment()
                .setClassLoader(this.class.getClassLoader())
                .setDefaultMultipartConfig(new MultipartConfigElement(uploadLocation, maxFileSize, maxRequestSize, fileSizeThreshold))
                .setTempDir(File.createTempDir()) // 这里上传文件的时候，如果 location 空，会用到。但设置了 location，这里就必须设置。
                .setContextPath(context)
                .setDeploymentName("grin.war")
                .addServlets(Servlets.servlet("GrinServlet", GServlet.class).addMapping("/*"))
                .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSockets)
        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo)
        manager.deploy()
        HttpHandler handler = manager.start()
        handler = new EncodingHandler.Builder().build([:]).wrap(handler)

        def buider = Undertow.builder()
        buider.setIoThreads(ioThreads).setWorkerThreads(workerThreads)
        if (port != -1) buider.addHttpListener(port, host)
        if (httpsPort != -1) buider.addHttpsListener(httpsPort, host, buildSSL(jksPath, jksPwd))
        buider.setHandler(handler)
        Undertow server = buider.build()
        server.start()

        if (port != -1) log.info("start server @ http://${host}:${port}${context}")
        if (httpsPort != -1) log.info("start server @ https://${host}:${httpsPort}${context}")
    }

    SSLContext buildSSL(String jks, String pwd) {
        KeyStore serverKeyStore = KeyStore.getInstance("JKS")
        serverKeyStore.load(new FileInputStream(jks), pwd.toCharArray())
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(serverKeyStore, pwd.toCharArray())// 加载密钥储存器
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509")
        tmf.init(serverKeyStore)
        SSLContext sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null)
        return sslContext
    }

    /**
     * 启动应用
     * @param args
     */
    static void main(String[] args) {
        // App.init(null, 'prod') // 需要配置特定路径和环境的时候启用
        new Application().start()
    }
}
