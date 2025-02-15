package grin.web

import grin.app.GrinApplication
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.lang.reflect.InvocationTargetException

/**
 * 拦截器
 */
@Slf4j
class Interceptor {
    def statusCodeMessages = [
            400: '请求有错误，请检查访问的地址是否正确',
            401: '需要登录后访问此页面',
            403: '此地址已被禁止访问',
            404: '页面不存在',
            405: '请求方法错误',
            422: '数据验证错误，请检查提交的数据',
            500: '服务器内部错误，请稍后再试'
    ]

    boolean before(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }

    boolean after(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }

    /**
     * 处理异常
     * 曾经想把异常处理放到控制器里实现，但这会带来一个问题，当控制器不存在的时候，没法执行了。只能放在这里了。似乎之前也是因为这个问题挪到这里的。
     */
    void dealException(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        if (exception instanceof InvocationTargetException) exception = exception.getTargetException()
        log.warn("Exception: ${exception.getMessage()}")
        int status = (exception instanceof HttpException) ? exception.status : 500
        String message = exception.message ?: statusCodeMessages[status] ?: '服务器出现错误，轻稍后再试'
        if (status >= 500) exception.printStackTrace()
        response.status = status
        def accept = request.getHeader('Accept')
        GrinApplication app = GrinApplication.instance
        if (accept?.contains('json')) {
            response.setHeader("Content-Type", "application/json;charset=UTF-8")
            response.getWriter().write(JsonOutput.toJson([success: false, message: message]))
        } else {
            String view = status == 404 ? app.config.views.notFound : app.config.views.error
            app.template.render(request, response, view, [exception: exception, message: message])
        }
    }
}