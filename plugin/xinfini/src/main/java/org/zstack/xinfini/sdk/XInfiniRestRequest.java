package org.zstack.xinfini.sdk;


import org.zstack.xinfini.XInfiniApiCategory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface XInfiniRestRequest {
    String path();
    String method();
    Class responseClass();
    XInfiniApiCategory category();
    String version() default "v1";
    boolean sync() default true;
    boolean hasBody() default false;
}
