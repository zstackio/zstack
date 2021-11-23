package org.zstack.header.core.encrypt;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Author: DaoDao
 * @Date: 2021/10/28
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SignedText {
    AppointColumn[] appointColumnName() default {};//specify special in the table
    String tableName();
    String primaryKey();
    String[] signedColumnName();
}
