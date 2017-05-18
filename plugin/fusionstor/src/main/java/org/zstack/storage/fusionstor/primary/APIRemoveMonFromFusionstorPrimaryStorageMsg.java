package org.zstack.storage.fusionstor.primary;

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
        path = "/primary-storage/fusionstor/{uuid}/mons",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveMonFromFusionstorPrimaryStorageEvent.class
)
public class APIRemoveMonFromFusionstorPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> monHostnames;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonHostnames() {
        return monHostnames;
    }

    public void setMonHostnames(List<String> monHostnames) {
        this.monHostnames = monHostnames;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
 
    public static APIRemoveMonFromFusionstorPrimaryStorageMsg __example__() {
        APIRemoveMonFromFusionstorPrimaryStorageMsg msg = new APIRemoveMonFromFusionstorPrimaryStorageMsg();
        msg.setUuid(uuid());
        msg.setMonHostnames(asList("192.168.0.100"));
        return msg;
    }

}
