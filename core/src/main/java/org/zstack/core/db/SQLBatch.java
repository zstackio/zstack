package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by xing5 on 2017/3/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class SQLBatch {
    @Autowired
    protected DatabaseFacade databaseFacade;

    protected <K> K persist(K k) {
        databaseFacade.getEntityManager().persist(k);
        return k;
    }

    protected <K> K merge(K k) {
        databaseFacade.getEntityManager().merge(k);
        return k;
    }

    protected void remove(Object k) {
        databaseFacade.getEntityManager().remove(k);
    }

    protected void flush() {
        databaseFacade.getEntityManager().flush();
    }

    protected <K> K findByUuid(String uuid, Class<K> clz) {
        return databaseFacade.getEntityManager().find(clz, uuid);
    }

    protected <K> K reload(K k) {
        flush();
        databaseFacade.getEntityManager().refresh(k);
        return k;
    }

    protected abstract void scripts();

    protected SQL sql(String text) {
        return SQL.New(text);
    }

    protected UpdateQuery sql(Class clz) {
        return SQL.New(clz);
    }

    protected SQL sql(String text, Class clz) {
        return SQL.New(text, clz);
    }

    protected Q q(Class clz) {
        return Q.New(clz);
    }

    @Transactional
    private void _execute() {
        scripts();
    }

    @DeadlockAutoRestart
    public void execute() {
        _execute();
    }
}
