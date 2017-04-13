package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2017/1/11.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SQL {
    @Autowired
    private DatabaseFacade dbf;

    private String sql;
    private Query query;

    private Class entityClass;
    private Map<String, Object> params = new HashMap<>();
    private Integer first;
    private Integer max;
    private LockModeType lockMode;

    private SQL(String sql) {
        this.sql = sql;
        query = dbf.getEntityManager().createQuery(this.sql);
    }

    private SQL(String sql, Class returnClass) {
        this.sql = sql;
        entityClass = returnClass;
        query = dbf.getEntityManager().createQuery(this.sql, returnClass);
    }

    public SQL param(String key, Object o) {
        query.setParameter(key, o);
        params.put(key, o);
        return this;
    }

    public SQL offset(int offset) {
        query.setFirstResult(offset);
        first = offset;
        return this;
    }

    public SQL limit(int max) {
        query.setMaxResults(max);
        this.max = max;
        return this;
    }

    public SQL lock(LockModeType mode) {
        query.setLockMode(mode);
        lockMode = mode;
        return this;
    }

    @Transactional(readOnly = true)
    private List transactionalList() {
        rebuildQueryInTransaction();
        return query.getResultList();
    }

    public <T> List<T> list()  {
        return transactionalList();
    }

    @Transactional(readOnly = true)
    private <K> K transactionalFind() {
        rebuildQueryInTransaction();
        List lst = query.getResultList();
        return lst.isEmpty() ? null : (K) lst.get(0);
    }

    private void rebuildQueryInTransaction() {
        query = entityClass == null ? dbf.getEntityManager().createQuery(sql) : dbf.getEntityManager().createQuery(sql, entityClass);
        if (first != null) {
            query.setFirstResult(first);
        }
        if (lockMode != null) {
            query.setLockMode(lockMode);
        }
        if (max != null) {
            query.setMaxResults(max);
        }
        for (Map.Entry<String, Object> e : params.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
        }
    }

    public <K> K find() {
        return transactionalFind();
    }

    @Transactional
    private int transactionalExecute() {
        rebuildQueryInTransaction();
        int ret = query.executeUpdate();
        dbf.getEntityManager().flush();
        return ret;
    }

    public int execute() {
        return transactionalExecute();
    }

    public static UpdateQuery New(Class entityClass) {
        return UpdateQuery.New(entityClass);
    }

    public static SQL New(String sql) {
        return new SQL(sql);
    }

    public static SQL New(String sql, Class returnClass) {
        return new SQL(sql, returnClass);
    }
}
