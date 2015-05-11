package org.zstack.core.workflow;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class NoRollbackFlow implements Flow {
    public void rollback(FlowTrigger trigger, Map data) {
        trigger.rollback();
    }
}
