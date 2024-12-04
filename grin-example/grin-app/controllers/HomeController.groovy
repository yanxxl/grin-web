import grin.web.Controller
import grin.web.HttpException
import groovy.util.logging.Slf4j

import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong

@Slf4j
class HomeController extends Controller {
    static AtomicLong count = new AtomicLong()

    def index() {
        render('/index', [:])
    }

    def hello() {
        long c = count.getAndIncrement()
        session.count = c
        render("Hello,${params.name ?: 'World'}! ${c}")
    }

    def ex() {
        throw new HttpException(500, "异常测试")
        // throw new HttpException(404)
    }

    def param() {
        log.info("request uri: ${request.getRequestURI()}")
        // json([params: params, headers: headers])
        json([1, 2, 3])
    }

    def html() {
        log.info("request uri: ${request.getRequestURI()}")
        html.html {
            head {}
            body {
                p("您好")
            }
        }
        // html.body {
        //     p("hhh")
        // }
    }

    def mem() {
        def mem = ManagementFactory.memoryMXBean
        def heapUsage = mem.heapMemoryUsage
        def nonHeapUsage = mem.nonHeapMemoryUsage
        def r = """MEMORY:
HEAP STORAGE:
\tcommitted = $heapUsage.committed
\tinit = $heapUsage.init
\tmax = $heapUsage.max
\tused = $heapUsage.used
NON-HEAP STORAGE:
\tcommitted = $nonHeapUsage.committed
\tinit = $nonHeapUsage.init
\tmax = $nonHeapUsage.max
\tused = $nonHeapUsage.used
"""
        println(r)
        println(ManagementFactory.getRuntimeMXBean().getName()) // 这个通常要 5s，有些夸张
        render(r)
    }
}
