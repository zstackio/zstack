package org.zstack.header.log;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by mingjian.deng on 2018/5/22.
 * only hide or skip dump msg logger, usually uses for password
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoLogging {
    enum Type {
        Auto,
        Mask;

        public boolean auto() {
            return this == Auto;
        }

        public boolean mask() {
            return this == Mask;
        }
    }

    Type type() default Type.Mask;
}
