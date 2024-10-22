package book

import grin.datastore.Entity
import groovy.transform.ToString

/**
 * Author
 */
@ToString(includeNames = true, excludes = ['errors'])
class Author implements Entity<Author> {
    long id // must,long or String
    String name
    Date createAt = new Date()

    @Override
    String toString() {
        return name
    }
}

