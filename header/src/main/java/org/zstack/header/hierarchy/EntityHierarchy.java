package org.zstack.header.hierarchy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EntityHierarchy {
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Friend {
        Class type();
        String myField();
        String targetField();
    }

    Class parent();

    String myField();

    String targetField();

    Friend[] friends() default {};
}
