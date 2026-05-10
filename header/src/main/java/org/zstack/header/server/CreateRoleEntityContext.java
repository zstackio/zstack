package org.zstack.header.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to {@link PhysicalServerRoleProvider#createRoleEntity(CreateRoleEntityContext)}
 * (role SPI v3, 2026-04-16). Provides the RoleProvider with everything it needs to forward to the
 * underlying role-module API (AddKVMHostMsg / AddBareMetal2ChassisMsg / K8s sync) with
 * {@code serverUuid} set — so Path 1 (PS-first) and Path 2 (legacy Add*Msg with null serverUuid)
 * converge on the same internal flow.
 */
public class CreateRoleEntityContext {
    private String serverUuid;
    private String clusterUuid;
    private String zoneUuid;
    private String managementIp;
    private String oobAddress;
    private Integer oobPort;
    private String oobUsername;
    private String oobPassword;
    private String accountUuid;
    private String preGeneratedRoleUuid;
    private Map<String, String> roleConfig = new HashMap<>();

    public String getServerUuid() { return serverUuid; }
    public CreateRoleEntityContext setServerUuid(String serverUuid) { this.serverUuid = serverUuid; return this; }

    public String getClusterUuid() { return clusterUuid; }
    public CreateRoleEntityContext setClusterUuid(String clusterUuid) { this.clusterUuid = clusterUuid; return this; }

    public String getZoneUuid() { return zoneUuid; }
    public CreateRoleEntityContext setZoneUuid(String zoneUuid) { this.zoneUuid = zoneUuid; return this; }

    public String getManagementIp() { return managementIp; }
    public CreateRoleEntityContext setManagementIp(String managementIp) { this.managementIp = managementIp; return this; }

    public String getOobAddress() { return oobAddress; }
    public CreateRoleEntityContext setOobAddress(String oobAddress) { this.oobAddress = oobAddress; return this; }

    public Integer getOobPort() { return oobPort; }
    public CreateRoleEntityContext setOobPort(Integer oobPort) { this.oobPort = oobPort; return this; }

    public String getOobUsername() { return oobUsername; }
    public CreateRoleEntityContext setOobUsername(String oobUsername) { this.oobUsername = oobUsername; return this; }

    public String getOobPassword() { return oobPassword; }
    public CreateRoleEntityContext setOobPassword(String oobPassword) { this.oobPassword = oobPassword; return this; }

    public String getAccountUuid() { return accountUuid; }
    public CreateRoleEntityContext setAccountUuid(String accountUuid) { this.accountUuid = accountUuid; return this; }

    public String getPreGeneratedRoleUuid() { return preGeneratedRoleUuid; }
    public CreateRoleEntityContext setPreGeneratedRoleUuid(String preGeneratedRoleUuid) { this.preGeneratedRoleUuid = preGeneratedRoleUuid; return this; }

    public Map<String, String> getRoleConfig() { return roleConfig; }
    public CreateRoleEntityContext setRoleConfig(Map<String, String> roleConfig) {
        this.roleConfig = roleConfig == null ? new HashMap<>() : roleConfig;
        return this;
    }
}
