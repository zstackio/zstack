package org.zstack.core.gc;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 8/5/2015.
 */
class GCContextInternal {
    String runnerClassName;
    String contextClassName;
    LinkedHashMap context;
    TimeUnit timeUnit;
    long interval;

    String toJson() {
        return JSONObjectUtil.toJsonString(this);
    }

    public GCContextInternal() {
    }

    public GCContextInternal(GarbageCollectorVO vo) {
        GCContextInternal i = JSONObjectUtil.toObject(vo.getContext(), GCContextInternal.class);
        runnerClassName = i.runnerClassName;
        contextClassName = i.contextClassName;
        context = i.context;
        timeUnit = i.timeUnit;
        interval = i.interval;
    }

    public GCContext toGCContext() {
        try {
            GCContext ctx = new GCContext();
            if (contextClassName != null) {
                ctx.setContextClass(Class.forName(contextClassName));
            }
            if (context != null) {
                ctx.setContext(JSONObjectUtil.rehashObject(context.get("context"), ctx.getContextClass()));
            }
            ctx.setInterval(interval);
            ctx.setTimeUnit(timeUnit);
            ctx.setRunnerClass(Class.forName(runnerClassName));
            return ctx;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
