package grin.datastore


import groovy.util.logging.Slf4j

import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 数据定义
 */
@Slf4j
class DDL {

    /**
     * 数据库中的表
     * @return
     */
    static Map<String, List<String>> tables() {
        Connection connection = DB.dataSource.connection
        def resultSet = connection.metaData.getColumns(connection.catalog, connection.schema, null, null)
        def result = [:]

        while (resultSet.next()) {
            def tableName = resultSet.getString('TABLE_NAME').toLowerCase()
            def columnName = resultSet.getString('COLUMN_NAME').toLowerCase()
            if (!result[tableName]) result[tableName] = []
            result[tableName] << columnName
        }

        return result
    }

    /**
     * 创建表
     * @param entityClass
     */
    static create(List<Class<Entity>> entityClassList) {
        entityClassList.each {
            DB.executeSql(entityCreateSql(it))
        }
        updateForeignKey(entityClassList)
    }

    /**
     * 删除表
     * @param entityClassList
     * @return
     */
    static drop(List<Class<Entity>> entityClassList) {
        entityClassList.each {
            DB.executeSql("drop table if exists ${Utils.findTableName(it)} cascade")
        }
    }

    /**
     * 重建表
     * @param entityClassList
     * @return
     */
    static dropAndCreate(List<Class<Entity>> entityClassList) {
        drop(entityClassList)
        create(entityClassList)
    }

    /**
     * 更新表
     * 缺少的表或者列，补齐，并不删除内容，只提醒。
     * @param entityClassList
     * @return
     */
    static update(List<Class<Entity>> entityClassList) {
        def tables = grin.datastore.DDL.tables()
        def existTableNames = tables.keySet() //已经存在的表
        def targetTableNames = entityClassList.collect { Utils.findTableName(it) } // 要更新的表
        def noUseTableNames = existTableNames - targetTableNames // 不再用的表,提醒
        if (noUseTableNames) log.warn("多余的表 ${noUseTableNames}")
        def newTableNames = targetTableNames - existTableNames // 新的表，创建
        if (newTableNames) create(entityClassList.findAll { Utils.findTableName(it) in newTableNames })
        def oldTableNames = existTableNames.intersect(targetTableNames) //旧表，更新列
        entityClassList.findAll { Utils.findTableName(it) in oldTableNames }.each { entity ->
            def tableName = Utils.findTableName(entity)
            def columnsNow = tables[tableName]
            def properties = Utils.findPropertiesToPersist(entity)
            def columnsWill = properties.collect { Utils.findColumnName(entity, it) }
            if (columnsNow - columnsWill) log.warn("多余的列 ${columnsNow - columnsWill}")
            properties.each {
                def columnName = Utils.findColumnName(entity, it)
                if (!(columnName in columnsNow)) {
                    DB.executeSql("alter table ${tableName} add column ${columnSql(entity, it, columnName)}")
                }
            }
        }
        updateForeignKey(entityClassList)
    }


    /**
     * 更新外键
     * 一般放到创建多个表后执行，避免依赖还没有创建的表。
     * @param entityClassList
     * @return
     */
    static updateForeignKey(List<Class<Entity>> entityClassList) {
        Connection connection = DB.dataSource.connection
        entityClassList.each {
            def entity = it
            def resultSet = connection.metaData.getImportedKeys(connection.catalog, connection.schema, Utils.findTableName(entity))
            def columns = [] // 已经存在外键的列列表，避免重复添加。pg 重复添加会产生多个。
            while (resultSet.next()) {
                columns.add(resultSet.getString("FKCOLUMN_NAME"))
            }
            Utils.findPropertiesToPersist(entity).each {
                def propertyType = entity.getDeclaredField(it).type
                if (propertyType.interfaces.contains(Entity) && !columns.contains(Utils.findColumnName(entity, it))) {
                    DB.executeSql("alter table ${Utils.findTableName(entity)} add foreign key (${Utils.findColumnName(entity, it)}) " +
                            "references ${Utils.findTableName(propertyType)}")
                }
            }
        }
    }

    /**
     * 实体类创建表 SQL
     * @param entityClass
     * @return
     */
    static String entityCreateSql(Class<Entity> entityClass) {
        def fields = Utils.findPropertiesToPersist(entityClass)
        def tableName = Utils.findTableName(entityClass)

        return """
create table ${tableName}(
${fields.collect { "        ${columnSql(entityClass, it, Utils.findColumnName(entityClass, it))}" }.join(',\n')}
)
"""
    }

    /**
     * 列 SQL
     * @param entityClass
     * @param propertyName
     * @param columnName
     * @return
     */
    static String columnSql(Class<Entity> entityClass, String propertyName, String columnName) {
        def cls = entityClass.getDeclaredField(propertyName).type
        def type = '未知类型'
        def constraint = ''

        // id
        if (propertyName == 'id') {
            if (cls in [int, Integer]) {
                return "id serial primary key"
            } else if (cls in [long, Long]) {
                return "id bigserial primary key"
            } else if (cls == String) {
                return "id varchar[32] primary key"
            } else {
                throw new Exception("id 必须是 整数或者字符串")
            }
        }

        // log.debug("列类型 ${entityClass.name} ${propertyName} ${columnName} - ${cls.name}")

        def nullable = Utils.getEntityConstraintValue(entityClass, propertyName, 'Nullable')
        constraint += nullable ? 'default null' : 'not null'

        if (cls == String) {
            def maxLength = Utils.getEntityConstraintValue(entityClass, propertyName, 'MaxLength')
            type = maxLength ? "varchar(${maxLength})" : 'varchar'
        } else if (cls in [boolean, Boolean]) {
            type = 'boolean'
        } else if (cls in [byte, Byte, short, Short, int, Integer]) {
            type = 'integer'
        } else if (cls in [long, Long]) {
            type = 'bigint'
        } else if (cls in [float, Float]) {
            type = 'real'
        } else if (cls in [double, Double]) {
            type = 'double precision'
        } else if (cls == BigDecimal) {
            type = 'decimal'
        } else if (cls == Date) {
            type = 'timestamp'
        } else if (cls == LocalDate) {
            type = 'date'
        } else if (cls == LocalTime) {
            type = 'time'
        } else if (cls == LocalDateTime) {
            type = 'timestamp'
        } else if (cls in [List, Map]) {
            type = 'varchar'
        } else if (cls.interfaces.contains(Entity)) {
            def c = cls.getDeclaredField('id').type
            if (c in [int, Integer]) {
                type = 'integer'
            } else if (c in [long, Long]) {
                type = 'bigint'
            } else if (c == String) {
                type = 'varchar(32)'
            } else {
                throw new Exception("${propertyName} 的 id 必须是 整数或者字符串")
            }
        } else {
            throw new Exception("未支持的 Java 类型 ${cls.name}")
        }


        return "${columnName} ${type} ${constraint}"
    }

}
