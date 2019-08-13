package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/authentication/file/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIDeleteVerificationFileEvent.class
)
public class APIDeleteVerificationFileMsg extends APIMessage {
    @APIParam
    private String path;
    @APIParam
    private String node;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }


    public static APIDeleteVerificationFileMsg __example__(){
        APIDeleteVerificationFileMsg msg = new APIDeleteVerificationFileMsg();
        msg.setPath("/usr/local/zstack/VERSION");
        msg.setNode("");
        return msg;
    }
}
