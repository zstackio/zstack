package org.zstack.header.core.workflow;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Map;

public abstract class CheckFlow implements Flow {
    private Flow targetFlow;

    public abstract Class<? extends Flow> getTargetFlow();

    public abstract void checkSkipTargetFlow(Map data, ReturnValueCompletion<Boolean> completion);

    @Override
    public void run(FlowTrigger trigger, Map data) {
        try {
            targetFlow = getTargetFlow().newInstance();
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        checkSkipTargetFlow(data, new ReturnValueCompletion<Boolean>(trigger) {
            @Override
            public void success(Boolean skip) {
                if (skip) {
                    trigger.next();
                } else {
                    targetFlow.run(trigger, data);
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        targetFlow.rollback(trigger, data);
    }
}
