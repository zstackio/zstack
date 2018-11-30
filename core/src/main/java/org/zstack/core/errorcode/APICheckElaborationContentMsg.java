package org.zstack.core.errorcode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/21.
 */
@RestRequest(
        path = "/errorcode/elaborations/check",
        method = HttpMethod.POST,
        responseClass = APICheckElaborationContentReply.class,
        parameterName = "params"
)
public class APICheckElaborationContentMsg extends APISyncCallMessage {
    @APIParam
    private String elaborateFile;

    public String getElaborateFile() {
        return elaborateFile;
    }

    public void setElaborateFile(String elaborateFile) {
        this.elaborateFile = elaborateFile;
    }

    public static APICheckElaborationContentMsg __example__() {
        APICheckElaborationContentMsg msg = new APICheckElaborationContentMsg();

        msg.setElaborateFile("/tmp/Host1.json");

        return msg;
    }
}
