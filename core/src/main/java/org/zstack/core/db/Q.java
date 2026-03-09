package org.zstack.core.db;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.utils.DebugUtils;

import jakarta.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by xing5 on 2016/12/31.
 */
public class Q {
    private SimpleQueryImpl q;

    @SuppressWarnings("unchecked")
    private Q(Class clz) {
        q = new SimpleQueryImpl(clz);
    }

    public Q select(Object... attrs) {
        q.select(attrs);
        return this;
    }

    public Q orderBy(Object attr, SimpleQuery.Od order) {
        q.orderBy(attr, order);
        return this;
    }

    public Q orderByAsc(Object attr) {
        q.orderBy(attr, SimpleQuery.Od.ASC);
        return this;
    }

    public Q orderByDesc(Object attr) {
        q.orderBy(attr, SimpleQuery.Od.DESC);
        return this;
    }

    public Q groupBy(Object attr) {
        q.groupBy(attr);
        return this;
    }

    public Q limit(int limit) {
        q.setLimit(limit);
        return this;
    }

    public Q start(int start) {
        q.setStart(start);
        return this;
    }

    @Transactional(readOnly = true)
    public boolean isExists() {
        return q._count() > 0;
    }

    @Transactional(readOnly = true)
    public Long count() {
        return q._count();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> T find() {
        return (T) q._find();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> List<T> list() {
        List<T> res = q._list();
        if (res != null)
            return res;
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <K> K findValue() {
        return (K) q._findValue();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <K> List<K> listValues() {
        List<K> res = q._listValue();
        if (res != null)
            return res;
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public Tuple findTuple() {
        return q._findTuple();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Tuple> listTuple() {
        List<Tuple> res = q._listTuple();
        if (res != null)
            return res;
        return Collections.emptyList();
    }

    public Q eq(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.EQ, val);
        return this;
    }

    public Q notEq(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.NOT_EQ, val);
        return this;
    }

    public Q in(Object attr, Collection val) {
        DebugUtils.Assert(CollectionUtils.isNotEmpty(val), "Op.IN value cannot be null or empty");
        q.add(attr, SimpleQuery.Op.IN, val);
        return this;
    }

    public QueryMore in(Object attr, Q subQuery) {
        return toQueryMore().in(attr, subQuery);
    }

    public Q notIn(Object attr, Collection val) {
        q.add(attr, SimpleQuery.Op.NOT_IN, val);
        return this;
    }

    public QueryMore notIn(Object attr, Q subQuery) {
        return toQueryMore().notIn(attr, subQuery);
    }

    public Q isNull(Object attr) {
        q.add(attr, SimpleQuery.Op.NULL);
        return this;
    }

    public Q notNull(Object attr) {
        q.add(attr, SimpleQuery.Op.NOT_NULL);
        return this;
    }

    public Q gt(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.GT, val);
        return this;
    }

    public Q gte(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.GTE, val);
        return this;
    }

    public Q lt(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.LT, val);
        return this;
    }

    public Q lte(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.LTE, val);
        return this;
    }

    public Q like(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.LIKE, val);
        return this;
    }

    public Q notLike(Object attr, Object val) {
        q.add(attr, SimpleQuery.Op.NOT_LIKE, val);
        return this;
    }

    public static Q New(Class clz) {
        return new Q(clz);
    }

    public static QueryMore New(Class<?> clz, Class<?>... more) {
        return new QueryMore(clz, more);
    }

    public QueryMore toQueryMore() {
        return q.toQueryMore();
    }
}
