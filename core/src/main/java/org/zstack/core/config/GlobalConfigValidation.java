package org.zstack.core.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GlobalConfigValidation {
    long[] inNumberRange() default {};
    long numberGreaterThan() default Long.MIN_VALUE;
    long numberLessThan() default Long.MAX_VALUE;
    boolean notNull() default true;
    boolean notEmpty() default true;
    String[] validValues() default {};
}
