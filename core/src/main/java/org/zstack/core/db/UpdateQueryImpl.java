package org.zstack.core.db;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/6/29.
 */
@Configurable(preConstruction=true,autowire= Autowire.BY_TYPE,dependencyCheck=true)
public class UpdateQueryImpl implements UpdateQuery {
    private static CLogger logger = Utils.getLogger(UpdateQueryImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    private Class entityClass;
    private Map<SingularAttribute, Object> setValues = new HashMap<>();
    private Map<SingularAttribute, Cond> andConditions = new HashMap<>();

    private class Cond {
        SingularAttribute attr;
        Op op;
        Object val;
    }

    @Override
    public UpdateQuery entity(Class clazz) {
        entityClass = clazz;
        return this;
    }

    @Override
    public UpdateQuery set(SingularAttribute attr, Object val) {
        if (setValues.containsKey(attr)) {
            throw new CloudRuntimeException(String.format("unable to set a column[%s] twice", attr.getName()));
        }

        setValues.put(attr, val);
        return this;
    }

    @Override
    public UpdateQuery condAnd(SingularAttribute attr, Op op, Object... vals) {
        if (andConditions.containsKey(attr)) {
            throw new CloudRuntimeException(String.format("unable to add the same condition[%s] twice", attr.getName()));
        }

        Cond cond = new Cond();
        cond.attr = attr;
        cond.op = op;
        cond.val = vals;

        andConditions.put(attr, cond);
        return this;
    }

    private String where() {
        if (andConditions.isEmpty()) {
            return null;
        }

        List<String> condstrs = new ArrayList<>();
        for (Cond cond : andConditions.values()) {
            if (Op.IN == cond.op || Op.NOT_IN == cond.op) {
                condstrs.add(String.format("vo.%s%s(:%s)", cond.attr.getName(), cond.op.toString(), cond.attr.getName()));
            } else if (Op.NULL == cond.op || Op.NOT_NULL == cond.op) {
                condstrs.add(String.format("vo.%s %s", cond.attr.getName(), cond.op));
            } else {
                condstrs.add(String.format("vo.%s %s :%s", cond.attr.getName(), cond.op.toString(), cond.attr.getName()));
            }
        }

        return StringUtils.join(condstrs, " ");
    }

    private void fillConditions(Query q) {
        for (Cond cond : andConditions.values()) {
            if (Op.IN == cond.op || Op.NOT_IN == cond.op) {
                q.setParameter(cond.attr.getName(), cond.val);
            } else {
                q.setParameter(cond.attr.getName(), ((Object[]) cond.val)[0]);
            }
        }
    }

    @Override
    @Transactional
    public void delete() {
        DebugUtils.Assert(entityClass!=null, "entity class cannot be null");

        StringBuilder sb = new StringBuilder(String.format("DELETE FROM %s vo", entityClass.getSimpleName()));
        String where = where();
        if (where != null) {
            sb.append(String.format(" WHERE %s", where));
        }

        String sql = sb.toString();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }

        Query q = dbf.getEntityManager().createQuery(sql);

        if (where != null) {
            fillConditions(q);
        }

        q.executeUpdate();
    }

    @Override
    @Transactional
    public void update() {
        DebugUtils.Assert(entityClass!=null, "entity class cannot be null");

        StringBuilder sb = new StringBuilder(String.format("UPDATE %s vo", entityClass.getSimpleName()));
        List<String> setters = new ArrayList<>();
        for (Map.Entry<SingularAttribute, Object> e : setValues.entrySet())  {
            setters.add(String.format("vo.%s=:%s", e.getKey().getName(), e.getKey().getName()));
        }

        sb.append(String.format(" SET %s ", StringUtils.join(setters, ",")));

        String where = where();
        if (where != null) {
            sb.append(String.format(" WHERE %s", where));
        }

        String sql = sb.toString();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }

        Query q = dbf.getEntityManager().createQuery(sql);
        for (Map.Entry<SingularAttribute, Object> e : setValues.entrySet())  {
            q.setParameter(e.getKey().getName(), e.getValue());
        }

        if (where != null) {
            fillConditions(q);
        }

        q.executeUpdate();
    }
}
