package org.zstack.core.gc;

/**
 * Created by frank on 8/5/2015.
 */
public interface GCFacade {
    void schedule(GCContext context);

    void scheduleImmediately(GCContext context);
}
