package grin.datastore


import groovy.transform.ToString
import org.h2.jdbcx.JdbcDataSource

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static grin.datastore.validate.Validators.*

/**
 * 书 & 作者
 */
class Book implements Entity<Book> {
    Integer id
    Author author
    String title
    String description
    int pageCount
    long wordCount
    float weight
    double weightDouble
    BigDecimal price
    String forPeople = '青少年'
    Date publishAt
    List<String> tags
    Map<String, Object> metaData
    LocalDate datePublished
    LocalTime timePublished
    LocalDateTime dateCreated
    LocalDateTime lastUpdated
    // boolean isDeleted

    static transients = []
    static constraints = [
            title       : [minLength(3), maxLength(5, '太长了'), matches('Y.{2}')],
            description : [nullable(), blank(), maxLength(10000)],
            pageCount   : [min(1)],
            wordCount   : [min(1),
                           validator('超过 100 字，太长了') { String fieldName, Object fieldValue, Entity<?> entity ->
                               fieldValue < 100
                           }],
            weight      : [nullable()],
            weightDouble: [nullable()],
            price       : [max(5.5), min(1.0)],
            forPeople   : [inList(['儿童', '青少年', '成年人'])],
            tags        : [nullable()],
            metaData    : [nullable()],
    ]
}

@ToString
class Author implements Entity<Author> {
    Long id
    String name
    String description
    String period
}

class EntityTest extends GroovyTestCase {

    def dataSource = new JdbcDataSource(url: "jdbc:h2:~/h2db/grin-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH", user: 'sa', password: '')
    // def dataSource = new DruidDataSource(url: "jdbc:postgresql://localhost:5432/grin_dev", username: 'postgres', password: 'pg@local')

    void testValidator() {
        Book book = new Book(price: 3)
        book.validate()
        println(book.errors)
    }

    void testGetConstraints() {
        println(Utils.getEntityConstraintValue(Book, 'title', 'Nullable'))
        println(Utils.getEntityConstraintValue(Book, 'author', 'MaxLength'))
        println(Utils.getEntityConstraintValue(Book, 'forPeople', 'InList'))
    }

    void testDDL() {
        DB.dataSource = dataSource
        // DDL.tables().each { println(it) }
        // DDL.drop([Book, Author])
        // DDL.create([Book, Author])
        // DDL.update([Book, Author])
        DDL.tables().each { println(it) }
    }

    void testAPI() {
        DB.dataSource = dataSource

        // Author author = new Author()
        // author.name = "Tom"
        // author.save()
        // println(author.toMap())

        // println(Author.get(1))
        // println(Author.get('1'))
        // println(Author.getAll([1]))
        // println(Author.getAll([1,'1']))
        // println(Author.getAll([1,2,3],['id','name']))
        // println(Author.where([name:'Jack']).get())
        // println(Author.where([id:1,name:'Jack']).get())
        // println(Author.where([id:[1,2,'3']]).list([order:[id:-1,name:1]]))

        // Book book = new Book()
        Book.list([order: 'wordCount asc,id desc'])
    }

    void testUpdateChangedList() {
        DB.dataSource = dataSource

        Author author = new Author()
        author.name = "Tom"
        author.description = 'somebody'
        author.save()
        println(author.toMap())

        Author author1 = Author.get(author.id)
        author1.name = "Jack"
        author1.save()
        println(author1.toMap())
    }

    void testList() {
        DB.dataSource = dataSource

        def authors = Author.list()
        authors.each { println(it.toMap()) }
    }
}
