package book

import grin.web.Controller
import grin.web.HttpException
import groovy.util.logging.Slf4j

/**
 * Book
 * something about this controller
 */
@Slf4j
class BookController extends Controller {
    def index() {
        int limit = Math.min(Math.max(params.limit ? params.limit.toInteger() : 10, 1), 100)
        int offset = Math.max(params.offset ? params.offset.toInteger() : 0, 0)
        String order = params.order ?: 'id desc'

        def list = Book.list([order: order, limit: limit, offset: offset])
        def count = Book.count()

        list.fetch()

        // 分页参数计算
        int current = offset / limit + 1
        int pageCount = ((count % limit == 0) ? (count / limit) : (count / limit) + 1) ?: 1
        def preLink = "?limit=${limit}&offset=${offset - limit}"
        def nextLink = "?limit=${limit}&offset=${offset + limit}"

        render('index', [list: list, count: count, pagination: [current: current, pageCount: pageCount, preLink: preLink, nextLink: nextLink]])
    }

    def show() {
        Book book = Book.get(params.id)

        if (!book) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        book.fetch()

        render('show', [book: book])
    }

    def create() {
        Book book = Book.from(params)
        render('create', [book: book])
    }

    def save() {
        Book book = Book.from(params)
        book.validate()

        if (book.errors) {
            render('create', [book: book])
        } else {
            if (book.save()) {
                redirect("show/${book.id}")
            } else {
                render('create', [book: book])
            }
        }
    }

    def edit() {
        Book book = Book.get(params.id)

        if (!book) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        render('edit', [book: book])
    }

    def update() {
        Book book = Book.get(params.id)

        if (!book) {
            throw new HttpException(404, "请求的内容不存在或者已删除")
            return
        }

        book.bind(params)
        book.validate()

        if (book.errors) {
            render('edit', [book: book])
        } else {
            if (book.save()) {
                redirect("show/${book.id}")
            } else {
                render('edit', [book: book])
            }
        }
    }

    def delete() {
        Book book = Book.get(params.id)
        book.delete()
        redirect("../index")
    }
}


