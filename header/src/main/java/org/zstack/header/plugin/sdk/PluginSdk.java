package org.zstack.header.plugin.sdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type of the feature plugin SDK. Feature plugins may only depend on SDK types and the
 * allowed core types; an SDK type only gets additions within a major SDK version.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginSdk {
    /**
     * the SDK version that introduced the type, e.g. "1.0"
     */
    String since();
}
