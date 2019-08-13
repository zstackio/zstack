package org.zstack.authentication.checkfile;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/authentication/file/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIAddVerificationFileEvent.class
)
public class APIAddVerificationFileMsg extends APIMessage {
    @APIParam
    private String path;

    @APIParam
    private String node;

    @APIParam
    private String type;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APIAddVerificationFileMsg __example__(){
        APIAddVerificationFileMsg msg = new APIAddVerificationFileMsg();
        msg.setPath("/usr/local/zstack/VERSION");
        msg.setNode("");
        msg.setType("md5");
        return msg;
    }

}
