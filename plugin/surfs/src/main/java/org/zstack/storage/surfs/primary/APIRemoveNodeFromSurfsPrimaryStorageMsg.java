package org.zstack.storage.surfs.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by frank on 8/6/2015.
 */
@RestRequest(
        path = "/primary-storage/surfs/{uuid}/nodes",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveNodeFromSurfsPrimaryStorageEvent.class
)
public class APIRemoveNodeFromSurfsPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> nodeHostnames;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getNodeHostnames() {
        return nodeHostnames;
    }

    public void setNodeHostnames(List<String> nodeHostnames) {
        this.nodeHostnames = nodeHostnames;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
 
    public static APIRemoveNodeFromSurfsPrimaryStorageMsg __example__() {
        APIRemoveNodeFromSurfsPrimaryStorageMsg msg = new APIRemoveNodeFromSurfsPrimaryStorageMsg();
        msg.setUuid(uuid());
        msg.setNodeHostnames(asList("192.168.0.100"));
        return msg;
    }

}
