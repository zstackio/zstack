package org.zstack.query;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * @ Author : yh.w
 * @ Date   : Created in 20:59 2020/5/28
 */
public class ZQLQueryMsg extends NeedReplyMessage {
    private String zql;

    private SessionInventory sessionInventory;

    public SessionInventory getSessionInventory() {
        return sessionInventory;
    }

    public void setSessionInventory(SessionInventory sessionInventory) {
        this.sessionInventory = sessionInventory;
    }

    public String getZql() {
        return zql;
    }

    public void setZql(String zql) {
        this.zql = zql;
    }

}
