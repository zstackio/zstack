package org.zstack.header.server;

/**
 * Context for role auto-association matching (FR-027).
 * Carries fields from the external resource to match against existing PhysicalServerVO.
 */
public class RoleMatchContext {
    private String serialNumber;
    private String managementIp;
    private String zoneUuid;
    private String oobAddress;
    private String roleUuid;
    private ServerRoleType roleType;
    private SchedulingMode schedulingMode;

    public String getSerialNumber() {
        return serialNumber;
    }

    public RoleMatchContext setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public RoleMatchContext setManagementIp(String managementIp) {
        this.managementIp = managementIp;
        return this;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public RoleMatchContext setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
        return this;
    }

    public String getOobAddress() {
        return oobAddress;
    }

    public RoleMatchContext setOobAddress(String oobAddress) {
        this.oobAddress = oobAddress;
        return this;
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public RoleMatchContext setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
        return this;
    }

    public ServerRoleType getRoleType() {
        return roleType;
    }

    public RoleMatchContext setRoleType(ServerRoleType roleType) {
        this.roleType = roleType;
        return this;
    }

    public SchedulingMode getSchedulingMode() {
        return schedulingMode;
    }

    public RoleMatchContext setSchedulingMode(SchedulingMode schedulingMode) {
        this.schedulingMode = schedulingMode;
        return this;
    }
}
