package org.zstack.core.gc;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 8/5/2015.
 */
public class EventBasedGCPersistentContext<T> extends AbstractEventBasedGCContext<T> {
    private Class runnerClass;
    private Class<T> contextClass;

    public EventBasedGCPersistentContext() {
    }

    public EventBasedGCPersistentContext(EventBasedGCPersistentContext other) {
        this.runnerClass = other.runnerClass;
        this.contextClass = other.contextClass;
    }

    public Class getRunnerClass() {
        return runnerClass;
    }

    public void setRunnerClass(Class runnerClass) {
        this.runnerClass = runnerClass;
    }

    public Class<T> getContextClass() {
        return contextClass;
    }

    public void setContextClass(Class<T> contextClass) {
        this.contextClass = contextClass;
    }

    EventBasedGCPersistentContextInternal toInternal() {
        EventBasedGCPersistentContextInternal i = new EventBasedGCPersistentContextInternal();
        if (contextClass != null) {
            i.contextClassName = contextClass.getName();
        }
        if (context != null) {
            i.context = JSONObjectUtil.rehashObject(map(e("context", context)), LinkedHashMap.class);
        }
        i.runnerClassName = runnerClass.getName();
        i.triggers  = triggers;
        i.contextName = name;
        return i;
    }
}
