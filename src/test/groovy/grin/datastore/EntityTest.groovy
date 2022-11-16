package grin.datastore


import org.h2.jdbcx.JdbcDataSource

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static grin.datastore.validate.Validators.*

class EntityTest extends GroovyTestCase {

    /**
     * 书
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

    class Author implements Entity<Author> {
        Long id
        String name
        // String description
    }

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
        DB.dataSource = new JdbcDataSource(url: "jdbc:h2:~/h2db/test;MODE=PostgreSQL", user: 'sa', password: '')

        // println("Tables")
        // DDL.tablesMetaData().each { println(it) }
        // println("Columns")
        // DDL.columnsMetaData().each {println(it)}

        DDL.dropTables([Book, Author])
        // DDL.createTables([Book, Author])
        DDL.updateTables([Book, Author])
        DDL.tablesStatus().each { println(it) }
    }
}