package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by kayo on 2018/8/3.
 */
@RestResponse(fieldsTo = "apis")
public class APICheckResourcePermissionReply extends APIReply {
    private List<String> apis;

    public List<String> getApis() {
        return apis;
    }

    public void setApis(List<String> apis) {
        this.apis = apis;
    }
}
