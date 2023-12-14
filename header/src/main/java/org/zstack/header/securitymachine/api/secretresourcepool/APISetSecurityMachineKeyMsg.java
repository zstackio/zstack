package org.zstack.header.securitymachine.api.secretresourcepool;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolConstant;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;

/**
 * Created by LiangHanYu on 2021/11/9 19:20
 */
@Action(category = SecretResourcePoolConstant.CATEGORY)
@RestRequest(
        path = "/secret-resource-pool-token/set/{uuid}/actions",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APISetSecurityMachineKeyEvent.class
)
public class APISetSecurityMachineKeyMsg extends APIMessage implements SecretResourcePoolMessage, APIAuditor {
    @APIParam(resourceType = SecretResourcePoolVO.class)
    private String uuid;

    @APIParam
    private String type;

    @APIParam
    private String tokenName;

    @APIParam(required = false)
    private boolean dryRun = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public static APISetSecurityMachineKeyMsg __example__() {
        APISetSecurityMachineKeyMsg msg = new APISetSecurityMachineKeyMsg();
        msg.setUuid(uuid());
        msg.setType("Test");
        msg.setTokenName("testToken");
        msg.setDryRun(false);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APISetSecurityMachineKeyMsg) msg).getUuid(), SecretResourcePoolVO.class);
    }

    @Override
    public String getSecretResourcePoolUuid() {
        return uuid;
    }
}
