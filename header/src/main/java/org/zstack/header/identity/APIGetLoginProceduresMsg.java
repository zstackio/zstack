package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by Wenhao.Zhang on 22/11/29
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/login-procedures",
        method = HttpMethod.GET,
        responseClass = APIGetLoginProceduresReply.class
)
public class APIGetLoginProceduresMsg extends APISessionMessage {
    @APIParam
    private String name;
    @APIParam
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APIGetLoginProceduresMsg __example__() {
        APIGetLoginProceduresMsg msg = new APIGetLoginProceduresMsg();
        msg.setName("test");
        msg.setType(AccountConstant.LOGIN_TYPE);
        return msg;
    }
}
