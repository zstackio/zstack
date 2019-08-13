package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/authentication/file/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRemoveVerificationFileEvent.class
)
public class APIRemoveVerificationFileMsg extends APIMessage {

    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public static APIRemoveVerificationFileMsg __example__(){
        APIRemoveVerificationFileMsg msg = new APIRemoveVerificationFileMsg();
        msg.setUuid("00001");
        return msg;
    }
}
