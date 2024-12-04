package grin.web

import grin.app.GrinApplication
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.WebContext
import org.thymeleaf.templateresolver.FileTemplateResolver
import org.thymeleaf.web.IWebExchange
import org.thymeleaf.web.servlet.JakartaServletWebApplication


/**
 * 模板
 */
class ThymeleafTemplate {
    TemplateEngine templateEngine

    ThymeleafTemplate(GrinApplication app) {
        templateEngine = new TemplateEngine()
        FileTemplateResolver resolver = new FileTemplateResolver()
        resolver.setPrefix(app.viewsDir.canonicalPath)
        resolver.setSuffix('.html')
        resolver.setCharacterEncoding('utf-8')
        resolver.setCacheable(!app.isDev())
        templateEngine.setTemplateResolver(resolver)
    }

    /**
     * 控制器外渲染
     * @param request
     * @param path
     * @param modal
     */
    void render(HttpServletRequest request, HttpServletResponse response, String path, Map model) {
        IWebExchange iWebExchange = JakartaServletWebApplication.buildApplication(request.servletContext).buildExchange(request, response)
        WebContext ctx = new WebContext(iWebExchange, request.getLocale())
        ctx.setVariables(model)
        templateEngine.process(path, ctx, response.getWriter())
    }

    /**
     * 控制器内渲染
     * @param controller
     * @param path
     * @param modal
     */
    void render(Controller controller, String view, Map model) {
        IWebExchange iWebExchange = JakartaServletWebApplication.buildApplication(controller.request.servletContext).buildExchange(controller.request, controller.response)
        WebContext ctx = new WebContext(iWebExchange, controller.request.getLocale())
        Map map = [app    : controller.app, controllerName: controller.controllerName, actionName: controller.actionName,
                   context: controller.context, request: controller.request, response: controller.response, session: controller.session, params: controller.params]
        map.putAll(model)
        // request 之类的变量，thymeleaf 是隐含地去用的，透明地传递过去，以备直接用。
        ctx.setVariables(map)
        String path = view.startsWith('/') ? view : "/${controller.controllerName}/${view}"
        templateEngine.process(path, ctx, controller.response.getWriter())
    }
}
