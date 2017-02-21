package org.zstack.test.integration

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by xing5 on 2017/2/21.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@interface Desc {
    String value() default ""
}