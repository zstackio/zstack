package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.SecurityMachineMessage;
import org.zstack.header.securitymachine.SecurityMachineVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/3 18:23
 */
@RestRequest(
        path = "/security-machines/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSecurityMachineEvent.class,
        isAction = true
)
public class APIUpdateSecurityMachineMsg extends APIMessage implements SecurityMachineMessage, APIAuditor {
    @APIParam(resourceType = SecurityMachineVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String managementIp;
    @APIParam(maxLength = 255, required = false)
    private String model;

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

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getSecurityMachineUuid() {
        return uuid;
    }

    public static APIUpdateSecurityMachineMsg __example__() {
        APIUpdateSecurityMachineMsg msg = new APIUpdateSecurityMachineMsg();
        msg.setUuid(uuid());
        msg.setDescription("example");
        msg.setManagementIp("192.168.0.1");
        msg.setName("example");
        msg.setModel("infoSecV2");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateSecurityMachineMsg) msg).getUuid(), SecurityMachineVO.class);
    }
}
