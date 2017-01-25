package org.zstack.appliancevm;

import org.zstack.header.message.APIListMessage;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class APIListApplianceVmMsg extends APIListMessage {
 
    public static APIListApplianceVmMsg __example__() {
        APIListApplianceVmMsg msg = new APIListApplianceVmMsg();
        msg.setUuids(list(uuid(),uuid()));

        return msg;
    }

}
