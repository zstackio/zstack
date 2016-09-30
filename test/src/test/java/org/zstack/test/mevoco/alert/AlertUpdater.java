package org.zstack.test.mevoco.alert;

import org.zstack.alert.APIUpdateAlertEvent;
import org.zstack.alert.APIUpdateAlertMsg;
import org.zstack.alert.AlertInventory;
import org.zstack.alert.AlertStatus;
import org.zstack.header.identity.SessionInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;

/**
 * Created by xing5 on 2016/9/9.
 */
public class AlertUpdater {
    private Api api;

    public String name;
    public String description;
    public AlertStatus status;
    public SessionInventory session;

    public AlertUpdater(Api api) {
        this.api = api;
    }

    public AlertInventory update(String uuid) throws ApiSenderException {
        ApiSender sender = api.getApiSender();
        APIUpdateAlertMsg msg = new APIUpdateAlertMsg();
        msg.setSession(session == null ? api.getAdminSession() : session);
        msg.setName(name);
        msg.setDescription(description);
        msg.setStatus(status.toString());
        msg.setUuid(uuid);
        APIUpdateAlertEvent evt = sender.send(msg, APIUpdateAlertEvent.class);
        return evt.getInventory();
    }
}
