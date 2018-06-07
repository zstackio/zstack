package org.zstack.header.message;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by mingjian.deng on 2018/5/22.
 * only skip dump msg logger, usually uses for password
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SkipLogger {
}
