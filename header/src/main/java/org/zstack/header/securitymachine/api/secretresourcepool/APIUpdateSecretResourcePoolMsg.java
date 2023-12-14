package org.zstack.header.securitymachine.api.secretresourcepool;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/3 17:37
 */
@RestRequest(
        path = "/secret-resource-pool/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSecretResourcePoolEvent.class,
        isAction = true
)
public class APIUpdateSecretResourcePoolMsg extends APIMessage implements SecretResourcePoolMessage, APIAuditor {
    @APIParam(resourceType = SecretResourcePoolVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255, required = false)
    private String model;
    @APIParam(numberRange = {6, 180}, required = false)
    private Integer heartbeatInterval;

    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static APIUpdateSecretResourcePoolMsg __example__() {
        APIUpdateSecretResourcePoolMsg msg = new APIUpdateSecretResourcePoolMsg();
        msg.setUuid(uuid());
        msg.setDescription("example");
        msg.setModel("infoSecV2");
        msg.setName("example");
        return msg;
    }

    @Override
    public String getSecretResourcePoolUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateSecretResourcePoolMsg) msg).getUuid(), SecretResourcePoolVO.class);
    }
}
