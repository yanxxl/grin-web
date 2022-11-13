package gun.datastore.validate

import gun.datastore.Entity

/**
 * 验证器
 */
abstract class Validator {
    Object value
    String message

    abstract boolean validate(String fieldName, Object fieldValue, Entity<?> entity)
}
