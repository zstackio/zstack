package org.zstack.core.gc;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 8/5/2015.
 */
class EventBasedGCPersistentContextInternal {
    String runnerClassName;
    String contextClassName;
    LinkedHashMap context;
    String code;
    String eventPath;

    String toJson() {
        return JSONObjectUtil.toJsonString(this);
    }

    public EventBasedGCPersistentContextInternal() {
    }

    public EventBasedGCPersistentContextInternal(GarbageCollectorVO vo) {
        EventBasedGCPersistentContextInternal i = JSONObjectUtil.toObject(vo.getContext(), EventBasedGCPersistentContextInternal.class);
        runnerClassName = i.runnerClassName;
        contextClassName = i.contextClassName;
        context = i.context;
        code = i.code;
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
            ctx.setCode(code);
            ctx.setEventPath(eventPath);
            return ctx;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
