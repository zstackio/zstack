package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.CreateSecretResourcePoolMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.zone.ZoneVO;

/**
 * Created by LiangHanYu on 2021/11/4 11:24
 */
public abstract class APICreateSecretResourcePoolMsg extends APICreateMessage implements CreateSecretResourcePoolMessage, APIAuditor {

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(maxLength = 255, required = false)
    private String model;

    /**
     * @desc define the type of resource pool, what type of resource pool can only add what type of security machine.
     * @choice CloudSecurityMachine：cloud security machine
     * OrdinarySecurityMachine：Physical security machine
     */
    @APIParam(validValues = {"CloudSecurityMachine", "OrdinarySecurityMachine"})
    private String type;

    @APIParam(numberRange = {6, 180})
    private Integer heartbeatInterval;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @Override
    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateSecretResourcePoolEvent evt = (APICreateSecretResourcePoolEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }

        return new Result(uuid, SecretResourcePoolVO.class);
    }
}
