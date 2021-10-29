package org.zstack.header.core.encrypt;

/**
 * @Author: DaoDao
 * @Date: 2021/10/28
 */
public @interface SignedText {
    AppointColumn[] appointColumnName() default {};
    String tableName();
    String[] signedColumnName();
}
