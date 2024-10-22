import grin.web.Interceptor

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AccessInterceptor extends Interceptor {

    boolean before(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }

    boolean after(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }
}
