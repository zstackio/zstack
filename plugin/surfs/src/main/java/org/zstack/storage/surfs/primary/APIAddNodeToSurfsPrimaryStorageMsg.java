package org.zstack.storage.surfs.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 8/6/2015.
 */
@RestRequest(
        path = "/primary-storage/surfs/{uuid}/nodes",
        method = HttpMethod.POST,
        responseClass = APIAddNodeToSurfsPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddNodeToSurfsPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = SurfsPrimaryStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> nodeUrls;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getNodeUrls() {
        return nodeUrls;
    }

    public void setNodeUrls(List<String> nodeUrls) {
        this.nodeUrls = nodeUrls;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
 
    public static APIAddNodeToSurfsPrimaryStorageMsg __example__() {
        APIAddNodeToSurfsPrimaryStorageMsg msg = new APIAddNodeToSurfsPrimaryStorageMsg();
        msg.setUuid(uuid());
        msg.setNodeUrls(asList("root:password@localhost/?monPort=7777"));
        return msg;
    }

}
