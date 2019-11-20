package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/authentication/file/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIDeleteVerificationFileEvent.class
)
public class APIDeleteVerificationFileMsg extends APIMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public static APIDeleteVerificationFileMsg __example__(){
        APIDeleteVerificationFileMsg msg = new APIDeleteVerificationFileMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
