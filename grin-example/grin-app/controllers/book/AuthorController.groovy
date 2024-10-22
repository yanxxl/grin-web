package book

import grin.web.Controller
import grin.web.HttpException
import groovy.util.logging.Slf4j

/**
 * Author
 * something about this controller
 */
@Slf4j
class AuthorController extends Controller {
    def index() {
        int limit = Math.min(Math.max(params.limit ? params.limit.toInteger() : 10, 1), 100)
        int offset = Math.max(params.offset ? params.offset.toInteger() : 0, 0)
        String order = params.order ?: 'id desc'

        def list = Author.list([order: order, limit: limit, offset: offset])
        def count = Author.count()

        list.fetch()

        // 分页参数计算
        int current = offset / limit + 1
        int pageCount = ((count % limit == 0) ? (count / limit) : (count / limit) + 1) ?: 1
        def preLink = "?limit=${limit}&offset=${offset - limit}"
        def nextLink = "?limit=${limit}&offset=${offset + limit}"

        render('index', [list: list, count: count, pagination: [current: current, pageCount: pageCount, preLink: preLink, nextLink: nextLink]])
    }

    def show() {
        Author author = Author.get(params.id)

        if (!author) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        author.fetch()

        render('show', [author: author])
    }

    def create() {
        Author author = Author.from(params)
        render('create', [author: author])
    }

    def save() {
        Author author = Author.from(params)
        author.validate()

        if (author.errors) {
            render('create', [author: author])
        } else {
            if (author.save()) {
                redirect("show/${author.id}")
            } else {
                render('create', [author: author])
            }
        }
    }

    def edit() {
        Author author = Author.get(params.id)

        if (!author) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        render('edit', [author: author])
    }

    def update() {
        Author author = Author.get(params.id)

        if (!author) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        author.bind(params)
        author.validate()

        if (author.errors) {
            render('edit', [author: author])
        } else {
            if (author.save()) {
                redirect("show/${author.id}")
            } else {
                render('edit', [author: author])
            }
        }
    }

    def delete() {
        Author author = Author.get(params.id)
        author.delete()
        redirect("../index")
    }
}


