package org.zstack.header.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be automatically traced with OpenTelemetry spans.
 * 
 * When applied to a method, the TracingAspect will create a span around
 * the method execution, capturing timing, errors, and context propagation.
 * 
 * Usage:
 * <pre>
 * {@code
 * @Traced
 * public void processMessage(Message msg) {
 *     // method body
 * }
 * 
 * @Traced(operationName = "custom-operation")
 * public Result doSomething() {
 *     // method body
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * Custom operation name for the span.
     * If empty, defaults to "ClassName.methodName"
     */
    String operationName() default "";
    
    /**
     * Additional attributes to include in the span.
     * Format: "key1=value1,key2=value2"
     */
    String attributes() default "";
    
    /**
     * Whether to record method parameters as span attributes.
     * Be cautious with sensitive data.
     */
    boolean recordParameters() default false;
}
