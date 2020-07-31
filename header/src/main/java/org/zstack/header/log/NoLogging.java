package org.zstack.header.log;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by MaJin on 2019/9/22.
 * mask sensitive info in log, usually uses for password.
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoLogging {
}
