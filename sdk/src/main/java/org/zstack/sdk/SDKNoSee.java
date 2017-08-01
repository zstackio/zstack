package org.zstack.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by MaJin on 2017-08-01.
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SDKNoSee {
}
