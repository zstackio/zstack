package org.zstack.core.db;

import org.zstack.core.db.SimpleQuery.Op;


import java.util.Collection;

/**
 * Created by xing5 on 2016/6/29.
 */
public interface UpdateQuery {
    UpdateQuery set(Object attr, Object val);

    UpdateQuery condAnd(Object attr, Op op, Object val);

    UpdateQuery eq(Object attr, Object val);

    UpdateQuery notEq(Object attr, Object val);

    UpdateQuery in(Object attr, Collection val);

    UpdateQuery notIn(Object attr, Collection val);

    UpdateQuery isNull(Object attr);

    UpdateQuery notNull(Object attr);

    UpdateQuery gt(Object attr, Object val);

    UpdateQuery gte(Object attr, Object val);

    UpdateQuery lt(Object attr, Object val);

    UpdateQuery lte(Object attr, Object val);

    UpdateQuery like(Object attr, Object val);

    UpdateQuery notLike(Object attr, Object val);

    void delete();

    int hardDelete();

    int update();

    static UpdateQuery New(Class entityClass) {
        return new UpdateQueryImpl().entity(entityClass);
    }
}
