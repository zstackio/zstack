package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.persistence.Tuple;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/12/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Q {
    @Autowired
    private DatabaseFacade dbf;

    private SimpleQuery q;

    private Q(Class clz) {
        q = dbf.createQuery(clz);
    }

    public Q select(SingularAttribute...attrs) {
        q.select(attrs);
        return this;
    }

    public Q orderBy(SingularAttribute attr, SimpleQuery.Od order) {
        q.orderBy(attr, order);
        return this;
    }

    public Q groupBy(SingularAttribute attr) {
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

    public <T> T find() {
        return (T) q.find();
    }

    public <T> List<T> list() {
        return q.list();
    }

    public <K> K findValue() {
        return (K) q.findValue();
    }

    public <K> List<K> listValues() {
        return q.listValue();
    }

    public Tuple findTuple() {
        return q.findTuple();
    }

    public List<Tuple> listTuple() {
        return q.listTuple();
    }

    public Q eq(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.EQ, val);
        return this;
    }

    public Q notEq(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.NOT_EQ, val);
        return this;
    }

    public Q in(SingularAttribute attr, Collection val) {
        q.add(attr, SimpleQuery.Op.IN, val);
        return this;
    }

    public Q notIn(SingularAttribute attr, Collection val) {
        q.add(attr, SimpleQuery.Op.NOT_IN, val);
        return this;
    }

    public Q isNull(SingularAttribute attr) {
        q.add(attr, SimpleQuery.Op.NULL);
        return this;
    }

    public Q notNull(SingularAttribute attr) {
        q.add(attr, SimpleQuery.Op.NOT_NULL);
        return this;
    }

    public Q gt(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.GT, val);
        return this;
    }

    public Q gte(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.GTE, val);
        return this;
    }

    public Q lt(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.LT, val);
        return this;
    }

    public Q lte(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.LTE, val);
        return this;
    }

    public Q like(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.LIKE, val);
        return this;
    }

    public Q notLike(SingularAttribute attr, Object val) {
        q.add(attr, SimpleQuery.Op.NOT_LIKE, val);
        return this;
    }

    public static Q New(Class clz) {
        return new Q(clz);
    }
}
