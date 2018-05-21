package org.zstack.header;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PackageAPIInfo {
    String APICategoryName() default "";
    boolean communityAvailable() default true;
    String productName() default "";
}
