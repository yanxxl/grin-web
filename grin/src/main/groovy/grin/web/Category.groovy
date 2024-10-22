package grin.web

import groovy.transform.CompileStatic

import javax.servlet.ServletContext
import javax.servlet.ServletRequest
import javax.servlet.http.HttpSession

@CompileStatic
class Category {
    static Object get(ServletContext context, String key) {
        return context.getAttribute(key)
    }

    static Object get(HttpSession session, String key) {
        return session.getAttribute(key)
    }

    static Object get(ServletRequest request, String key) {
        return request.getAttribute(key)
    }

    static Object getAt(ServletContext context, String key) {
        return context.getAttribute(key)
    }

    static Object getAt(HttpSession session, String key) {
        return session.getAttribute(key)
    }

    static Object getAt(ServletRequest request, String key) {
        return request.getAttribute(key)
    }

    static void set(ServletContext context, String key, Object value) {
        context.setAttribute(key, value)
    }

    static void set(HttpSession session, String key, Object value) {
        session.setAttribute(key, value)
    }

    static void set(ServletRequest request, String key, Object value) {
        request.setAttribute(key, value)
    }

    static void putAt(ServletContext context, String key, Object value) {
        context.setAttribute(key, value)
    }

    static void putAt(HttpSession session, String key, Object value) {
        session.setAttribute(key, value)
    }

    static void putAt(ServletRequest request, String key, Object value) {
        request.setAttribute(key, value)
    }
}
