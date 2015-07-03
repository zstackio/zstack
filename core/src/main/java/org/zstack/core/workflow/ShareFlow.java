package org.zstack.core.workflow;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;

import java.util.Map;

/**
 */
public abstract class ShareFlow implements Flow {
    private ShareFlowChain chain;

    void setChain(ShareFlowChain chain) {
        this.chain = chain;
    }

    protected void flow(Flow flow) {
        chain.install(flow);
    }

    protected void done(FlowDoneHandler handler) {
        chain.done(handler);
    }

    protected void error(FlowErrorHandler handler) {
        chain.error(handler);
    }

    @Override
    public final void run(FlowTrigger trigger, Map data) {
        trigger.next();
    }

    @Override
    public final void rollback(FlowTrigger trigger, Map data) {
        trigger.rollback();
    }

    public abstract void setup();
}
