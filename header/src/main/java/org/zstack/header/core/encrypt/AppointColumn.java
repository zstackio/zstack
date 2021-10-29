package org.zstack.header.core.encrypt;

import java.lang.annotation.*;

/**
 * @Author: DaoDao
 * @Date: 2021/10/29
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AppointColumn {
    String column();
    String vaule();
}
