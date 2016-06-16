package org.zstack.core.gc;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by frank on 8/5/2015.
 */
class EventBasedGCPersistentContextInternal {
    String contextName;
    String runnerClassName;
    String contextClassName;
    LinkedHashMap context;
    List<GCEventTrigger> triggers;

    String toJson() {
        return JSONObjectUtil.toJsonString(this);
    }

    public EventBasedGCPersistentContextInternal() {
    }

    public EventBasedGCPersistentContextInternal(GarbageCollectorVO vo) {
        EventBasedGCPersistentContextInternal i = JSONObjectUtil.toObject(vo.getContext(), EventBasedGCPersistentContextInternal.class);
        runnerClassName = i.runnerClassName;
        contextClassName = i.contextClassName;
        contextName = i.contextName;
        context = i.context;
        triggers = i.triggers;
    }

    public EventBasedGCPersistentContext toGCContext() {
        try {
            EventBasedGCPersistentContext ctx = new EventBasedGCPersistentContext();
            if (contextClassName != null) {
                ctx.setContextClass(Class.forName(contextClassName));
            }
            if (context != null) {
                ctx.setContext(JSONObjectUtil.rehashObject(context.get("context"), ctx.getContextClass()));
            }
            ctx.setRunnerClass(Class.forName(runnerClassName));
            ctx.setTriggers(triggers);
            ctx.setName(contextName);
            return ctx;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
