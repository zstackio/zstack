package org.zstack.core.groovy.gc

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by xing5 on 2017/3/1.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface GC {
}