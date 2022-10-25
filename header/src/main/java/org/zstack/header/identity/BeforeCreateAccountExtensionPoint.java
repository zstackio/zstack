package org.zstack.header.identity;

/**
 * @description: 创建账户前扩展点
 * @author: liupt@sugon.com
 * @time: 2022/10/9
 */
public interface BeforeCreateAccountExtensionPoint {
    void beforeCreateAccount(AccountInventory account);
}
