package org.zstack.header.core.encrypt;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * @Author: DaoDao
 * @Date: 2021/11/12
 */
@RestResponse(fieldsTo = {"all"})
public class APICheckBatchDataIntegrityReply extends APIReply {
    private Map<String, Boolean> resourceMap;

    public Map<String, Boolean> getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(Map<String, Boolean> resourceMap) {
        this.resourceMap = resourceMap;
    }

    public static APICheckBatchDataIntegrityReply __example__() {
        APICheckBatchDataIntegrityReply reply = new APICheckBatchDataIntegrityReply();
        Map<String, Boolean> map = new HashMap<>();
        map.put(uuid(), true);
        reply.setResourceMap(map);
        return reply;
    }

}
