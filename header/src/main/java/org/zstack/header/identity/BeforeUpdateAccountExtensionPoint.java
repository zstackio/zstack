package org.zstack.header.identity;

/**
 * @description:
 * @author: liupt@sugon.com
 * @time: 2022/10/9
 */
public interface BeforeUpdateAccountExtensionPoint {
    void beforeUpdateAccount(AccountInventory account);
}
