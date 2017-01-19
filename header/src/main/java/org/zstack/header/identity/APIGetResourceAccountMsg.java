package org.zstack.header.identity;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/8.
 */
public class APIGetResourceAccountMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> resourceUuids;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }
 
    public static APIGetResourceAccountMsg __example__() {
        APIGetResourceAccountMsg msg = new APIGetResourceAccountMsg();
        msg.setResourceUuids(list(uuid(), uuid()));

        return msg;
    }

}
