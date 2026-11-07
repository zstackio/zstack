package org.zstack.core.jsonlabel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.HardDeleteEntityExtensionPoint;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.SoftDeleteEntityExtensionPoint;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.AsyncThread;

import java.util.Collection;
import java.util.ArrayList;
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

    @AsyncThread
    private void delete(Collection ids) {
        // the resourceUuid must be in type of String
        if (!(ids.iterator().next() instanceof  String)) {
            return;
        }

        UpdateQuery q = UpdateQuery.New(JsonLabelVO.class);
        q.condAnd(JsonLabelVO_.resourceUuid, Op.IN, ids);
        q.delete();
    }

    private void deleteAfterCommit(Collection ids) {
        Collection snapshot = new ArrayList(ids);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    delete(snapshot);
                }
            });
        } else {
            delete(snapshot);
        }
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        if (entityClass.isAssignableFrom(JsonLabelVO.class)) {
            return;
        }

        if (!entityIds.isEmpty()) {
            deleteAfterCommit(entityIds);
        }
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

        if (!entityIds.isEmpty()) {
            deleteAfterCommit(entityIds);
        }
    }
}
