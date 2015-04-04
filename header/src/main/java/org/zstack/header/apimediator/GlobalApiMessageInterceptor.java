package org.zstack.header.apimediator;

import java.util.List;

public interface GlobalApiMessageInterceptor extends ApiMessageInterceptor {
    public enum InterceptorPosition {
        FRONT,
        END,
    }
    
    List<Class> getMessageClassToIntercept();
    
    InterceptorPosition getPosition();
}
