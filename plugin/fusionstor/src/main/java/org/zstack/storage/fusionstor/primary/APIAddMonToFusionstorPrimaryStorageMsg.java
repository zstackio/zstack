package org.zstack.storage.fusionstor.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.List;

/**
 * Created by frank on 8/6/2015.
 */
@RestRequest(
        path = "/primary-storage/fusionstor/{uuid}/mons",
        method = HttpMethod.POST,
        responseClass = APIAddMonToFusionstorPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddMonToFusionstorPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = FusionstorPrimaryStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> monUrls;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
}
