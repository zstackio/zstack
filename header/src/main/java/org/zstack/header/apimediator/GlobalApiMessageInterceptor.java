package org.zstack.header.apimediator;

import java.util.List;

public interface GlobalApiMessageInterceptor extends ApiMessageInterceptor {
    enum InterceptorPosition {
        FIRST(-5000),
        SYSTEM(-3000),
        FRONT(-1000),
        END(5000);

        public final int priority;
        InterceptorPosition(int priority) {
            this.priority = priority;
        }
    }

    List<Class> getMessageClassToIntercept();

    default InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    default int getPriority() {
        return getPosition().priority;
    }
}
