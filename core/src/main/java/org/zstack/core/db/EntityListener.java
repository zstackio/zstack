package org.zstack.core.db;

import org.zstack.core.Platform;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.*;

/**
 * Created by xing5 on 2016/3/12.
 */
public class EntityListener {
    private static CLogger logger = Utils.getLogger(EntityListener.class);

    private static DatabaseFacadeImpl dbf;

    public static synchronized DatabaseFacadeImpl getDataBaseFacade() {
        if (dbf == null) {
            dbf = Platform.getComponentLoader().getComponent(DatabaseFacadeImpl.class);
        }

        return dbf;
    }


    @PrePersist
    void onPrePersist(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.PRE_PERSIST, o);
    }

    @PostPersist
    void onPostPersist(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.POST_PERSIST, o);
    }

    @PostLoad
    void onPostLoad(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.POST_LOAD, o);
    }

    @PreUpdate
    void onPreUpdate(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.PRE_UPDATE, o);
    }

    @PostUpdate
    void onPostUpdate(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.POST_UPDATE, o);
    }

    @PreRemove
    void onPreRemove(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.PRE_REMOVE, o);
    }

    @PostRemove
    void onPostRemove(Object o) {
        getDataBaseFacade().entityEvent(EntityEvent.POST_REMOVE, o);
    }
}
