package org.zstack.header.identity;

import org.zstack.header.message.MessageReply;

/**
 * @Author: DaoDao
 * @Date: 2022/6/22
 */
public class CreateAccountReply extends MessageReply {
    private AccountInventory inventory;

    public AccountInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountInventory inventory) {
        this.inventory = inventory;
    }
}
