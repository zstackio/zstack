package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.SecurityMachineConstant;

/**
 * Created by LiangHanYu on 2021/11/16 10:56
 */
@Action(category = SecurityMachineConstant.CATEGORY)
@RestRequest(
        path = "/security-machine/encrypt/actions",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APISecurityMachineEncryptEvent.class
)
public class APISecurityMachineEncryptMsg extends APIMessage {
    @APIParam
    private String text;

    @APIParam
    private String algType;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAlgType() {
        return algType;
    }

    public void setAlgType(String algType) {
        this.algType = algType;
    }
}
