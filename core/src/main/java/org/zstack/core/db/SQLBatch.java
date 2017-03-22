package org.zstack.core.db;

import org.springframework.transaction.annotation.Transactional;

/**
 * Created by xing5 on 2017/3/4.
 */
public abstract class SQLBatch {
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
    public void execute() {
        scripts();
    }
}
