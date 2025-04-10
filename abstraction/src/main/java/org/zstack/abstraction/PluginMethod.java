package org.zstack.abstraction;

public @interface PluginMethod {
    boolean required() default true;
}
