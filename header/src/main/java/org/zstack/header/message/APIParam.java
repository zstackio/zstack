package org.zstack.header.message;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface APIParam {
    boolean operationTarget() default false;

    boolean required() default true;

    String[] validValues() default {};

    /**
     * Specify that this input parameter must be the value of some enumeration.
     * Note: "validEnums" and {@link #validValues()} cannot be set simultaneously.
     */
    Class<? extends Enum<?>>[] validEnums() default {};

    String validRegexValues() default "";

    Class resourceType() default Object.class;

    int maxLength() default Integer.MIN_VALUE;

    int minLength() default 0;

    boolean nonempty() default false;

    boolean nullElements() default false;

    boolean emptyString() default true;

    long[] numberRange() default {};

    String[] numberRangeUnit() default {};

    boolean checkAccount() default false;

    boolean noOwnerCheck() default false;

    boolean noTrim() default false;

    boolean successIfResourceNotExisting() default false;

    /**
        use @NoLogging instead
     */
    @Deprecated
    boolean password() default false;
}
