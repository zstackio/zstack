package org.zstack.header.apimediator;

import java.util.List;

public interface GlobalApiMessageInterceptor extends ApiMessageInterceptor {
    enum InterceptorPosition {
        SYSTEM,
        FRONT,
        END,
    }

    List<Class> getMessageClassToIntercept();

    InterceptorPosition getPosition();
}
