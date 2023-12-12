package org.zstack.header.securitymachine.api.secretresourcepool;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolState;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/5 14:01
 */
@RestRequest(
        path = "/secret-resource-pools/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeSecretResourcePoolStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeSecretResourcePoolStateMsg extends APIMessage implements SecretResourcePoolMessage, APIAuditor {
    @APIParam(resourceType = SecretResourcePoolVO.class)
    private String uuid;

    @APIParam(validValues = {"activate", "unactivate"})
    private String stateEvent;

    public APIChangeSecretResourcePoolStateMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public static APIChangeSecretResourcePoolStateMsg __example__() {
        APIChangeSecretResourcePoolStateMsg msg = new APIChangeSecretResourcePoolStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SecretResourcePoolState.Activated.toString());
        return msg;
    }

    @Override
    public String getSecretResourcePoolUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIChangeSecretResourcePoolStateMsg) msg).getUuid(), SecretResourcePoolVO.class);
    }
}
