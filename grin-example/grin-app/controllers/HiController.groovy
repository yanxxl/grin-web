import groovy.util.logging.Slf4j
import grin.web.Controller

/**
 * hi
 * something about this controller
 */
@Slf4j
class HiController extends Controller {
    def index() {
        render "你好,Grin !"
    }

    def upload() {
        render('upload', [:])
    }
}
