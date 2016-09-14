package org.zstack.core.jsonlabel;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.HardDeleteEntityExtensionPoint;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.SoftDeleteEntityExtensionPoint;
import org.zstack.core.db.UpdateQuery;

import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/9/14.
 */
public class JsonLabelResourceDeletionExtension implements SoftDeleteEntityExtensionPoint,
        HardDeleteEntityExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        // hook all
        return null;
    }

    private void delete(Collection ids) {
        UpdateQuery q = UpdateQuery.New();
        q.entity(JsonLabelVO.class);
        q.condAnd(JsonLabelVO_.resourceUuid, Op.IN, ids);
        q.delete();
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        if (entityClass.isAssignableFrom(JsonLabelVO.class)) {
            return;
        }

        delete(entityIds);
    }

    @Override
    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
        // hook all
        return null;
    }

    @Override
    public void postSoftDelete(Collection entityIds, Class entityClass) {
        if (entityClass.isAssignableFrom(JsonLabelVO.class)) {
            return;
        }

        delete(entityIds);
    }
}
