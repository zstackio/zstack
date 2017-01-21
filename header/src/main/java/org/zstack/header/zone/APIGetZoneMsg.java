package org.zstack.header.zone;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;

@Action(category = ZoneConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetZoneMsg extends APISyncCallMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIGetZoneMsg __example__() {
        APIGetZoneMsg msg = new APIGetZoneMsg();
        msg.setUuid(uuid());
        return msg;
    }

}
