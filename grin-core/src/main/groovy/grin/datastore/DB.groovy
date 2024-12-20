package grin.datastore

import groovy.sql.Sql
import groovy.util.logging.Slf4j

import javax.sql.DataSource

/**
 * DB 数据库
 */
@Slf4j
class DB {
    static DataSource dataSource // datasource,提供一个配置的入口，方便 grin 外部使用。

    /**
     * 获取 sql
     * @return
     */
    static Sql getSql() {
        new Sql(dataSource)
    }

    /**
     * with sql
     * @param closure
     * @return
     */
    static withSql(@DelegatesTo(SQL) Closure closure) {
        Sql sql = getSql()
        def result = closure.call(sql)
        sql.close()
        return result
    }

    /**
     * 用作闭包代理，便于 IDE 提示。
     */
    static class SQL {
        Sql sql
    }

    /**
     * 执行 sql 文件
     * 有些 entity 处理不了的问题，需要用一些 sql 来解决。可通过配置，启动时自动执行一下。
     * @param sqlFile
     */
    static void executeSqlFile(File sqlFile) {
        log.info("exec sql file ${sqlFile.name}")
        String s = sqlFile.text.trim()
        if (s) {
            s.split(';').each {
                executeSql(it)
            }
        }
    }

    /**
     * 执行 sql
     * @param sqlString
     */
    static void executeSql(String sqlString) {
        println("执行：\n${sqlString}")
        def start = System.currentTimeMillis()
        DB.withSql { Sql sql ->
            def r = sql.execute(sqlString)
            println("完成，${r ? '' : "影响了 ${sql.updateCount} 行，"}耗时 ${(System.currentTimeMillis() - start) / 1000000}ms")
        }
    }
}
