package org.zstack.header.core.encrypt;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2021/10/28
 */

@RestRequest(
        path = "/start/data/protection/",
        method = HttpMethod.POST,
        responseClass = APIStartDataProtectionEvent.class,
        parameterName = "params"
)
public class APIStartDataProtectionMsg extends APIMessage {
    @APIParam(required = false)
    private String encryptType = "InfosecEncryptDriver";

    public String getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(String encryptType) {
        this.encryptType = encryptType;
    }

    public static APIStartDataProtectionMsg __example__ () {
        APIStartDataProtectionMsg msg = new APIStartDataProtectionMsg();
        msg.setEncryptType("xxx");
        return msg;
    }
}
