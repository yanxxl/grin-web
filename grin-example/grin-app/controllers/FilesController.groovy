import grin.web.Controller
import grin.web.HttpException
import grin.web.LinkUtil
import groovy.util.logging.Slf4j

/**
 * 文件处理
 */
@Slf4j
class FilesController extends Controller {

    /**
     * asset
     * 需要路由定义配合 /asset/@file
     */
    void assets() {
        File assetFile
        if (app.isDev()) {
            assetFile = new File(app.assetDir, params.id)
        } else {
            assetFile = new File(app.assetBuildDir, params.id)
        }

        render(assetFile)
    }

    /**
     * 文件上传
     * @return
     */
    void upload() {
        List fileNames = []

        request.parts.each {
            if (it.submittedFileName) {
                String fileName = fileUUIDName(it.submittedFileName)
                it.write(fileName)
                fileNames << fileName
            }
        }

        def files = fileNames.collect { "/files/download/$it" }

        // 没有文件
        if (!files) {
            json(success: false, msg: 'no file')
            return
        }

        log.info("upload files by ${session.user} : ${files}")

        files = files.collect {
            LinkUtil.absolute(it)
        }

        if (files.size() == 1) {
            json(success: true, msg: 'ok', 'file': files[0])
        } else {
            json(success: true, msg: 'ok', 'files': files)
        }
    }

    /**
     * 文件下载
     */
    void download() {
        if (!params.id) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
        } else {
            render(new File("${app.config.fileUpload.location}/${params.id}"))
        }
    }

    /**
     * 站点静态文件
     */
    void 'static'() {
        if (!params.id) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        def f = new File("${app.staticDir}/${params.id}")
        if (f.isDirectory()) f = new File(f, 'index.html')
        if (!f.exists()) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        if (f.name.endsWith('.html')) {
            render(f.text)
        } else {
            render(f)
        }
    }

    /**
     * 产生一个 uuid 文件名，分析后缀
     * @param fileName
     * @return
     */
    static String fileUUIDName(String fileName) {
        def index = fileName.lastIndexOf('.')
        def postFix = ''
        if (index > 0) postFix = fileName.substring(index + 1)
        def uuid = UUID.randomUUID().toString().replace('-', '')
        return postFix ? "${uuid}.${postFix}" : uuid
    }
}
