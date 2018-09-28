package grace.datastore
/**
 * 提供实体类的基本功能
 */
trait Entity<D> {
    List errors = []

    /**
     * get
     * todo 如果带着 D ，编译的时候异常，找不到类型 D，这不太科学。
     * this 返回的是实现的类，这是 trait 的特性。
     * @param id
     * @return
     */
    static get(Serializable id) {
        EntityApiImpl.get(this, id)
    }

    /**
     * 列表
     * @param params
     * @return
     */
    static list(Map params = null) {
        EntityApiImpl.list(this, params)
    }

    /**
     * 计数
     * @return
     */
    static int count() {
        EntityApiImpl.count(this)
    }

    /**
     * 保存，更新或者插入
     * @return
     */
    D save() {
        EntityApiImpl.save(this)
    }

    /**
     * 删除
     * @return
     */
    boolean delete() {
        EntityApiImpl.delete(this)
    }

    /**
     * where 自定义条件查询
     * @param sql
     */
    static EntityApiImpl.Where where(String sql, List params = []) {
        new EntityApiImpl.Where(whereSql: sql, params: params, entityClass: this)
    }

    /**
     * 约束 Map
     * @return
     */
    static Map getConstraintMap() {
        EntityApiImpl.getConstraintMap(this)
    }

    /**
     * 验证约束
     * @return
     */
    boolean validate() {
        EntityApiImpl.validate(this)
    }
}