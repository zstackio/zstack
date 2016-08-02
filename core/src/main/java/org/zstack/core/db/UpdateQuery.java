package org.zstack.core.db;

import org.zstack.core.db.SimpleQuery.Op;

import javax.persistence.metamodel.SingularAttribute;

/**
 * Created by xing5 on 2016/6/29.
 */
public interface UpdateQuery {
    UpdateQuery entity(Class clazz);

    UpdateQuery set(SingularAttribute attr, Object val);

    UpdateQuery condAnd(SingularAttribute attr, Op op, Object val);

    void delete();

    void update();

    static UpdateQuery New() {
        return new UpdateQueryImpl();
    }
}
