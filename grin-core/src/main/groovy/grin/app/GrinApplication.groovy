package grin.app

import com.alibaba.druid.filter.Filter
import com.alibaba.druid.filter.logging.Slf4jLogFilter
import com.alibaba.druid.filter.stat.StatFilter
import com.alibaba.druid.pool.DruidDataSource
import com.alibaba.druid.sql.SQLUtils
import grin.datastore.DB
import grin.datastore.DDL
import grin.web.Interceptor
import grin.web.Route
import grin.web.ThymeleafTemplate
import grin.web.WebUtils
import groovy.util.logging.Slf4j

import java.lang.reflect.Method
import java.util.jar.JarFile

/**
 * Grin App
 * 定义规约目录等。
 */
@Slf4j
class GrinApplication {
    // env
    public static final String ENV_PROD = 'prod'
    public static final String ENV_DEV = 'dev'
    public static final String GRIN_ENV_NAME = 'GRIN_ENV'
    public static final List<String> GRIN_ENV_LIST = [ENV_DEV, ENV_PROD]
    // 目录结构
    public static final String APP_DIR = 'grin-app'
    public static final String APP_DOMAINS = 'domains'
    public static final String APP_CONTROLLERS = 'controllers'
    public static final String APP_WEBSOCKETS = 'websockets'
    public static final String APP_VIEWS = 'views'
    public static final String APP_CONFIG = 'conf'
    public static final String APP_INIT = 'init'
    public static final String APP_ASSETS = 'assets'
    public static final String APP_STATIC = 'static'
    public static final String APP_SCRIPTS = 'scripts'

    String environment
    File projectDir, appDir, domainsDir, controllersDir, websocketsDir, viewsDir, configDir, initDir, assetDir, staticDir, scriptDir
    List<File> allDirs

    ConfigObject config

    // 一些延迟初始化的属性
    private static GrinApplication _instance
    private GroovyScriptEngine _scriptEngine
    private ThymeleafTemplate _template

    // web 组件
    Map<String, String> controllers = [:]
    Map<String, Method> actions = [:]
    Interceptor interceptor
    List<Class> websockets = []
    List<Route> routes = []

    /**
     * 构造并初始化
     * @param projectRoot
     */
    private GrinApplication(File projectRoot, String env) {
        projectDir = projectRoot
        environment = env
        if (!(environment in GRIN_ENV_LIST)) throw new Exception("错误的运行环境值：${environment}，值必须是 ${GRIN_ENV_LIST} 之一。")

        appDir = new File(projectDir, APP_DIR)
        domainsDir = new File(appDir, APP_DOMAINS)
        controllersDir = new File(appDir, APP_CONTROLLERS)
        websocketsDir = new File(appDir, APP_WEBSOCKETS)
        viewsDir = new File(appDir, APP_VIEWS)
        configDir = new File(appDir, APP_CONFIG)
        initDir = new File(appDir, APP_INIT)
        assetDir = new File(appDir, APP_ASSETS)
        staticDir = new File(appDir, APP_STATIC)
        scriptDir = new File(appDir, APP_SCRIPTS)
        allDirs = [appDir, domainsDir, controllersDir, websocketsDir, viewsDir, configDir, initDir, assetDir, staticDir, scriptDir]

        // config
        config = loadConfig()
        log.info("start app @ ${projectDir.absolutePath} ${environment} ...")
    }

    /**
     * 初始化 APP
     * 在项目运行的主程序中调用
     * @param app 根据这个判断是否在 jar 中运行。如果在 jar 中，要从 jar 里读出 grin-app,并放到某个文件夹中去
     * @param root
     * @return
     */
    static GrinApplication init(Object app, String env = null) {
        if (_instance) throw new Exception("Grin app has initialized")
        // 如果运行在 jar 模式下，将项目文件放到一个文件夹下
        String jarPath = app.class.getProtectionDomain().getCodeSource()?.getLocation()?.toURI()?.getPath()
        if (jarPath?.endsWith('.jar')) {
            File rootDir = File.createTempDir()
            new JarFile(jarPath).entries().each {
                if (it.name.startsWith(APP_DIR)) {
                    if (it.isDirectory()) {
                        new File(rootDir, it.name).mkdir()
                    } else {
                        new File(rootDir, it.name) << app.class.getResourceAsStream(it.name)
                    }
                }
            }
            _instance = new GrinApplication(rootDir, (env ?: System.getenv(GRIN_ENV_NAME)) ?: ENV_PROD)
        } else {
            _instance = new GrinApplication(new File('.').canonicalFile, (env ?: System.getenv(GRIN_ENV_NAME)) ?: ENV_DEV)
        }
        return _instance
    }

    /**
     * 获取单例
     * @return
     */
    synchronized static GrinApplication getInstance() {
        if (!_instance) throw new Exception("没有调用 init 方法初始化！")
        return _instance
    }

    /**
     * 获取配置
     * 支持从用户目录加载配置文件 grin-app.properties ，以覆盖配置项，如数据库配置。
     * @return
     */
    ConfigObject loadConfig() {
        def configFile = new File(configDir, 'config.groovy')
        if (configFile.exists()) {
            def result = new ConfigSlurper(environment).parse(configFile.text)
            def userConfigFile = new File(System.getProperty('user.home'), 'grin-app.properties')
            if (userConfigFile.exists()) {
                log.info("发现用户目录存在配置文件 grin-app.properties，加载中...")
                result.merge(new ConfigSlurper().parse(userConfigFile.toURI().toURL()))
            }
            return result
        } else {
            throw new Exception("配置文件不存在")
        }
    }

    /**
     * 是否开发环境
     */
    boolean isDev() {
        ENV_DEV == environment
    }

    /**
     * 初始化项目目录结构
     * @param root
     * @return
     */
    void initDirs() {
        log.info("init grin app dirs @ ${projectDir.absolutePath}")
        allDirs.each {
            if (it.exists()) {
                log.info("${it.name} exists")
            } else {
                it.mkdirs()
                log.info("${it.name} mkdirs")
            }
        }
    }

    /**
     * 初始化数据库
     * 这个需要手动调用，在必要的地方。因为有时候初始化 APP 并不需要数据库，如运行创建领域类这样的命令。
     * @return
     */
    void initializeDB() {
        log.info("初始化数据库")
        def dataSource = new DruidDataSource(config.dataSource)
        if (config.logSql) {
            Filter sqlLog = new Slf4jLogFilter(statementExecutableSqlLogEnable: true)
            sqlLog.setStatementSqlFormatOption(new SQLUtils.FormatOption(true, false))
            dataSource.setProxyFilters([sqlLog, new StatFilter()])
        }
        DB.dataSource = dataSource
        if (config.dbCreate == 'create-drop') DDL.dropAndCreate(WebUtils.loadEntities(domainsDir))
        if (config.dbCreate == 'update') DDL.update(WebUtils.loadEntities(domainsDir))
        if (config.dbSql) DB.executeSqlFile(new File(scriptDir, config.dbSql as String))
        log.info("Tables：${DDL.tables().keySet()}")
    }

    /**
     * 初始化 web 组件
     * 如上面，这个也需要手动调用
     */
    void initializeWeb() {
        routes = WebUtils.loadRoutes(config.urlMapping)
        controllers = WebUtils.loadControllers(controllersDir)
        actions = WebUtils.loadActions(controllers)
        interceptor = WebUtils.findInterceptor(controllersDir) ?: new Interceptor()
        websockets = WebUtils.loadWebsockets(websocketsDir)
        log.info("初始化 web\nroutes:${routes}\ncontrollers:${controllers}\nactions:${actions}\nintercepter:${interceptor?.class}\nwebsockets:${websockets}")
    }

    /**
     * GSE 延时加载
     */
    synchronized GroovyScriptEngine getScriptEngine() {
        if (_scriptEngine) return _scriptEngine
        _scriptEngine = new GroovyScriptEngine(domainsDir.absolutePath, controllersDir.absolutePath, websocketsDir.absolutePath, scriptDir.absolutePath)
        return _scriptEngine
    }

    /**
     * 模板引擎
     * @return
     */
    synchronized ThymeleafTemplate getTemplate() {
        if (_template) return _template
        _template = new ThymeleafTemplate(this)
        return _template
    }
}
