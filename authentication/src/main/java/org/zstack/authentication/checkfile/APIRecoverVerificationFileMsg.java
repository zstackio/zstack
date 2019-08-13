package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/authentication/file/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRecoverVerificationFileEvent.class
)
public class APIRecoverVerificationFileMsg extends APIMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public static APIRecoverVerificationFileMsg __example__(){
        APIRecoverVerificationFileMsg msg = new APIRecoverVerificationFileMsg();
        msg.setUuid("00001");
        return msg;
    }
}
