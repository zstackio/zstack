package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * @Author : jingwang
 * @create 2023/4/25 2:33 PM
 */
@RestResponse(fieldsTo = "all")
public class APIGetHostWebSshUrlReply extends APIReply {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static APIGetHostWebSshUrlReply __example__() {
        APIGetHostWebSshUrlReply reply = new APIGetHostWebSshUrlReply();
        reply.setUrl("ws://{{ip}}:8888/ws?id=140147795208568");
        return reply;
    }
}
