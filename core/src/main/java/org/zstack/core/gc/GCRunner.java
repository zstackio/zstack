package org.zstack.core.gc;

/**
 * Created by frank on 8/5/2015.
 */
public interface GCRunner {
    void run(GCContext context, GCCompletion completion);
}
