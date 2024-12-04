import grin.web.Interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class AccessInterceptor extends Interceptor {

    boolean before(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }

    boolean after(HttpServletRequest request, HttpServletResponse response, String controllerName, String actionName) {
        return true
    }
}
