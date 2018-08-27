package org.zstack.core.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoAsyncSafe {
}
