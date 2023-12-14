package org.zstack.header.securitymachine.api.secretresourcepool;


import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/3 17:37
 */
@RestRequest(
        path = "/secret-resource-pool/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSecretResourcePoolEvent.class
)

public class APIDeleteSecretResourcePoolMsg extends APIDeleteMessage implements SecretResourcePoolMessage, APIAuditor {
    @APIParam(resourceType = SecretResourcePoolVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteSecretResourcePoolMsg __example__() {
        APIDeleteSecretResourcePoolMsg msg = new APIDeleteSecretResourcePoolMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getSecretResourcePoolUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteSecretResourcePoolMsg) msg).getUuid(), SecretResourcePoolVO.class);
    }
}
