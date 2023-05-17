package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author : jingwang
 * @create 2023/4/25 11:29 AM
 */
@RestRequest(
        path = "/hosts/webssh",
        method = HttpMethod.POST,
        responseClass = APIGetHostWebSshUrlEvent.class,
        parameterName = "params"
)
public class APIGetHostWebSshUrlMsg extends APIMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
