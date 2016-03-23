package org.zstack.core.gc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/3/22.
 */
public abstract class AbstractEventBasedGCContext<T> extends AbstractGCContext<T> {
    protected List<GCEventTrigger> triggers = new ArrayList<GCEventTrigger>();

    public AbstractEventBasedGCContext() {
    }

    public AbstractEventBasedGCContext(AbstractEventBasedGCContext other) {
        super(other);
        triggers = other.triggers;
    }

    public void addTrigger(GCEventTrigger t) {
        triggers.add(t);
    }

    public List<GCEventTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<GCEventTrigger> triggers) {
        this.triggers = triggers;
    }
}
