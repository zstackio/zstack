package org.zstack.header.securitymachine.api.securitymachine;


import org.zstack.header.securitymachine.AddSecurityMachineMessage;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.securitymachine.SecurityMachineVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.zone.ZoneVO;

/**
 * Created by LiangHanYu on 2021/11/14 16:18
 */
public abstract class APIAddSecurityMachineMsg extends APICreateMessage implements AddSecurityMachineMessage, APIAuditor {

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(emptyString = false)
    private String managementIp;

    @APIParam(maxLength = 255)
    private String model;

    /**
     * @desc define the type of resource pool, what type of resource pool can only add what type of security machine.
     * @choice CloudSecurityMachine：cloud security machine
     * OrdinarySecurityMachine：Physical security machine
     */
    @APIParam(validValues = {"CloudSecurityMachine", "OrdinarySecurityMachine"})
    private String type;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(resourceType = SecretResourcePoolVO.class)
    private String secretResourcePoolUuid;

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
    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    @Override
    public String getSecretResourcePoolUuid() {
        return secretResourcePoolUuid;
    }

    public void setSecretResourcePoolUuid(String secretResourcePoolUuid) {
        this.secretResourcePoolUuid = secretResourcePoolUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APIAddSecurityMachineEvent evt = (APIAddSecurityMachineEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }

        return new Result(uuid, SecurityMachineVO.class);
    }

}
