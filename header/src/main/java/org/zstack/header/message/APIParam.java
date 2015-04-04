package org.zstack.header.message;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface APIParam {
	boolean required() default true;
    String[] validValues() default {};
    Class resourceType() default Object.class;
    int maxLength() default Integer.MIN_VALUE;
    boolean nonempty() default false;
    long[] numberRange() default {};
}
