package org.zstack.core.aspect;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.utils.DebugUtils;

/**
 */
public aspect CompletionSingleCallAspect {
    void around(AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.Completion+.success()) {
        if (!completion.getSuccessCalled().compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("Completion.success() is mistakenly called twice");
            return;
        }

        proceed(completion);
    }

    void around(AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.Completion+.fail(*)) {
        if (!completion.getFailCalled().compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("Completion.fail() is mistakenly called twice");
            return;
        }

        proceed(completion);
    }

    void around(AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.ReturnValueCompletion+.success(*)) {
        if (!completion.getSuccessCalled().compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("ReturnValueCompletion.success() is mistakenly called twice");
            return;
        }

        proceed(completion);
    }

    void around(AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.ReturnValueCompletion+.fail(*)) {
        if (!completion.getFailCalled().compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("ReturnValueCompletion.fail() is mistakenly called twice");
            return;
        }

        proceed(completion);
    }

    void around(AbstractCompletion completion) : this(completion) && execution(void org.zstack.header.core.NoErrorCompletion+.done()) {
        if (!completion.getSuccessCalled().compareAndSet(false, true)) {
            DebugUtils.dumpStackTrace("NoErrorCompletion.done() is mistakenly called twice");
            return;
        }

        proceed(completion);
    }
}
