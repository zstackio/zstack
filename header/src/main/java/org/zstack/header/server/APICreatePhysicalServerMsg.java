package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreatePhysicalServerEvent.class
)
public class APICreatePhysicalServerMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(resourceType = ServerPoolVO.class)
    private String poolUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(maxLength = 255)
    private String managementIp;

    @APIParam(required = false, validValues = {"x86_64", "aarch64"})
    private String architecture;

    @APIParam(required = false, maxLength = 255)
    private String serialNumber;

    @APIParam(required = false)
    private String manufacturer;

    @APIParam(required = false)
    private String model;

    @APIParam(required = false, validValues = {"IPMI"})
    private String oobManagementType;

    @APIParam(required = false)
    private String oobAddress;

    @APIParam(required = false, numberRange = {1, 65535})
    private Integer oobPort;

    @APIParam(required = false)
    private String oobUsername;

    @NoLogging
    @APIParam(required = false, password = true)
    private String oobPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
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

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOobManagementType() {
        return oobManagementType;
    }

    public void setOobManagementType(String oobManagementType) {
        this.oobManagementType = oobManagementType;
    }

    public String getOobAddress() {
        return oobAddress;
    }

    public void setOobAddress(String oobAddress) {
        this.oobAddress = oobAddress;
    }

    public Integer getOobPort() {
        return oobPort;
    }

    public void setOobPort(Integer oobPort) {
        this.oobPort = oobPort;
    }

    public String getOobUsername() {
        return oobUsername;
    }

    public void setOobUsername(String oobUsername) {
        this.oobUsername = oobUsername;
    }

    public String getOobPassword() {
        return oobPassword;
    }

    public void setOobPassword(String oobPassword) {
        this.oobPassword = oobPassword;
    }

    public static APICreatePhysicalServerMsg __example__() {
        APICreatePhysicalServerMsg msg = new APICreatePhysicalServerMsg();
        msg.setName("server1");
        msg.setZoneUuid(uuid());
        msg.setPoolUuid(uuid());
        msg.setManagementIp("192.168.1.100");
        msg.setArchitecture("x86_64");
        msg.setSerialNumber("SN123456");
        msg.setManufacturer("Dell");
        msg.setModel("PowerEdge R750");
        msg.setOobManagementType("IPMI");
        msg.setOobAddress("192.168.1.200");
        msg.setOobPort(623);
        msg.setOobUsername("admin");
        msg.setOobPassword("password");
        return msg;
    }
}
