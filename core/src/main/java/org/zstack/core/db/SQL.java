package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.List;

/**
 * Created by xing5 on 2017/1/11.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SQL {
    @Autowired
    private DatabaseFacade dbf;

    private String sql;
    private Query query;
    private boolean useTransaction;

    private SQL(String sql) {
        this.sql = sql;
        query = dbf.getEntityManager().createQuery(this.sql);
    }

    private SQL(String sql, Class returnClass) {
        this.sql = sql;
        query = dbf.getEntityManager().createQuery(this.sql, returnClass);
    }

    public SQL transactional() {
        useTransaction = true;
        return this;
    }

    public SQL param(String key, Object o) {
        query.setParameter(key, o);
        return this;
    }

    public SQL first(int offset) {
        query.setFirstResult(offset);
        return this;
    }

    public SQL max(int max) {
        query.setMaxResults(max);
        return this;
    }

    public SQL lock(LockModeType mode) {
        query.setLockMode(mode);
        return this;
    }

    @Transactional(readOnly = true)
    private List transactionalList() {
        return query.getResultList();
    }

    public <T> List<T> list()  {
        return useTransaction ? transactionalList() : query.getResultList();
    }

    @Transactional(readOnly = true)
    private <K> K transactionalFind() {
        List lst = query.getResultList();
        return lst.isEmpty() ? null : (K) lst.get(0);
    }

    public <K> K find() {
        if (useTransaction) {
            return transactionalFind();
        } else {
            List lst = query.getResultList();
            return lst.isEmpty() ? null : (K) lst.get(0);
        }
    }

    @Transactional(readOnly = true)
    private int transactionalExecute() {
        return query.executeUpdate();
    }

    public int execute() {
        return useTransaction ? transactionalExecute() : query.executeUpdate();
    }

    public static SQL New(String sql) {
        return new SQL(sql);
    }

    public static SQL New(String sql, Class returnClass) {
        return new SQL(sql, returnClass);
    }
}
