package grin.datastore

import com.alibaba.druid.pool.DruidDataSource
import org.h2.jdbcx.JdbcDataSource

import java.sql.Connection
import java.sql.ResultSet

DB.dataSource = new JdbcDataSource(url: "jdbc:h2:~/h2db/grin-test;MODE=PostgreSQL", user: 'sa', password: '')
// DB.dataSource = new DruidDataSource(url: "jdbc:postgresql://localhost:5432/grin_dev", username: 'postgres', password: 'pg@local')

Connection connection = DB.dataSource.connection

println("${connection.metaData.driverName} - ${connection.metaData.driverVersion}")
println("${connection.metaData.databaseProductName} - ${connection.metaData.databaseProductVersion}")

def dumpResultSet(ResultSet resultSet, String title = '----------------') {
    println(title.center(50,'-'))
    def rmd = resultSet.getMetaData()
    def columnCount = rmd.columnCount
    println((1..columnCount).collect { rmd.getColumnName(it) })
    while (resultSet.next()) {
        println((1..columnCount).collect { resultSet.getObject(it) })
    }
}

// 表
// dumpResultSet(md.getTables(connection.catalog, connection.schema, null, null))

//引入的外键
def tables = []
({
    def resultSet = connection.metaData.getTables(connection.catalog, connection.schema, null, null)
    def rmd = resultSet.getMetaData()
    def columnCount = rmd.columnCount
    while (resultSet.next()) {
       tables.add(resultSet.getString("TABLE_NAME"))
    }
} as Closure).call()
tables.each {
    dumpResultSet(connection.getMetaData().getImportedKeys(connection.catalog, connection.schema, it),"$it ImportedKeys")
}

// 列
// dumpResultSet(md.getColumns(null, 'PUBLIC', null, null))