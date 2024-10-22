package book

import grin.datastore.Entity
import groovy.transform.ToString

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static grin.datastore.validate.Validators.*

/**
 * Book
 * */
@ToString(includeNames = true, excludes = ['errors'])
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
    static constraints = [title       : [minLength(3), maxLength(5, '太长了'), matches('Y.{2}')],
                          description : [nullable(), blank(), maxLength(10000)],
                          pageCount   : [min(1)],
                          wordCount   : [min(1),
                                         validator('超过 100 字，太长了') { String fieldName, Object fieldValue, Entity<?> entity -> fieldValue < 100 }],
                          weight      : [nullable()],
                          weightDouble: [nullable()],
                          price       : [max(5.5), min(1.0)],
                          forPeople   : [inList(['儿童', '青少年', '成年人'])],
                          tags        : [nullable()],
                          metaData    : [nullable()],]
}

