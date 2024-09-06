package grin.web

import grin.app.GrinApplication
import grin.datastore.Entity
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.json.StreamingJsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder

import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * 控制器基类
 * 包装请求，提供方便变量和方法使用数据
 */
@Slf4j
class Controller {
    // servlet
    HttpServletRequest request
    HttpServletResponse response
    Map<String, Object> pathParams
    Params params

    // app
    GrinApplication app = GrinApplication.instance

    // 控制器三大要素
    String controllerName
    String actionName
    String id

    /**
     * 初始化数据
     */
    void init(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName, String id, Map<String, Object> pathParams) {
        this.request = request
        this.response = response
        this.controllerName = controllerName
        this.actionName = actionName
        this.id = id
        this.pathParams = pathParams
    }

    /**
     * forward
     */
    void forward(String path) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(path)
        dispatcher.forward(request, response)
    }

    /**
     * redirect
     */
    void redirect(String location) throws IOException {
        response.sendRedirect(location)
    }

    /**
     * session
     * @return
     */
    HttpSession getSession() {
        request.getSession(true)
    }

    /**
     * context
     * @return
     */
    ServletContext getContext() {
        request.getServletContext()
    }

    /**
     * 获取参数，延时加载
     * @return
     */
    Params getParams() {
        if (params) return params

        params = new Params()
        if (id) params.put('id', id)
        if (pathParams) params.putAll(pathParams)
        for (Enumeration names = request.getParameterNames(); names.hasMoreElements();) {
            String name = (String) names.nextElement()
            String[] values = request.getParameterValues(name)
            if (values.length == 1) {
                params.put(name, values[0])
            } else {
                params.put(name, values)
            }
        }

        // json data
        String contentType = request.getHeader('content-type') ?: request.getHeader('Content-Type')
        if (contentType?.contains('application/json')) {
            try {
                if (!request.inputStream.isFinished()) {
                    String text = request.inputStream.text
                    if (text) {
                        def data = new JsonSlurper().parseText(text)
                        if (data instanceof Map) {
                            params.putAll(data)
                        } else {
                            params.put('json', data)
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("maybe json data,but throw an exception : ${e.getMessage()}")
                e.printStackTrace()
            }
        }

        return params
    }

    /**
     * headers
     * @return
     */
    Map getHeaders() {
        def headers = [:]
        request.headerNames.each {
            headers[it] = request.getHeader(it)
        }
        return headers
    }

    /**
     * json generator
     * @return
     */
    JsonGenerator getJsonGenerator() {
        response.setHeader("Content-Type", "application/json;charset=UTF-8")
        def jsonGenerator = new groovy.json.JsonGenerator.Options()
                .addConverter(Entity) { it.toMap() }
                .build()
        return jsonGenerator
    }

    /**
     * json builder
     * @param response
     * @return
     */
    StreamingJsonBuilder getJsonBuilder() {
        return new StreamingJsonBuilder(response.getWriter(), getJsonGenerator())
    }

    /**
     * html builder
     * @return
     */
    MarkupBuilder getHtmlBuilder() {
        new MarkupBuilder(response.getWriter())
    }

    /**
     * ThymeleafTemplate
     * @return
     */
    ThymeleafTemplate getTemplate() {
        return app.template
    }

    /**
     * 返回string
     * @param string
     */
    void render(String string) {
        response.getWriter().write(string)
    }

    /**
     * bytes
     * 无缓存，直接返回
     * @param bytes
     */
    void render(byte[] bytes) {
        response.reset()
        response.getOutputStream().write(bytes)
    }

    /**
     * 用以托底，render 任何对象
     * @param o
     */
    void render(Object o) {
        response.getWriter().write(o.toString())
    }

    /**
     * view and model
     * 默认 thymeleaf 渲染
     * @param view
     * @param model
     */
    void render(String view, Map model) {
        template.render(this, view, model)
    }

    /**
     * 文件处理
     * 开启了断点续传，缓存等。
     * @param file
     * @param cacheTime 默认 86400 秒 一天
     */
    void render(File file, int cacheTime = 86400) {
        response.reset()
        FileUtils.serveFile(request, response, file, cacheTime)
    }
}
