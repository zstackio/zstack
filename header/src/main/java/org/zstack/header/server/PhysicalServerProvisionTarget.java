package org.zstack.header.server;

import java.io.Serializable;
import java.util.Map;

public class PhysicalServerProvisionTarget implements Serializable {
    private String serverUuid;
    private String networkUuid;
    private String managementIp;
    private String oobAddress;
    private Integer oobPort;
    private String oobUsername;
    private String oobPassword;
    private String provisionNicMac;
    private String dhcpInterface;
    private String dhcpRangeStartIp;
    private String dhcpRangeEndIp;
    private String dhcpRangeNetmask;
    private String dhcpRangeGateway;
    private String osImageUuid;
    private String osDistribution;
    private String kickstartTemplate;
    private Map<String, String> customParams;
    private String jobUuid;

    public String getJobUuid() {
        return jobUuid;
    }

    public PhysicalServerProvisionTarget setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
        return this;
    }

    public String getServerUuid() {
        return serverUuid;
    }

    public PhysicalServerProvisionTarget setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
        return this;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public PhysicalServerProvisionTarget setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
        return this;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public PhysicalServerProvisionTarget setManagementIp(String managementIp) {
        this.managementIp = managementIp;
        return this;
    }

    public String getOobAddress() {
        return oobAddress;
    }

    public PhysicalServerProvisionTarget setOobAddress(String oobAddress) {
        this.oobAddress = oobAddress;
        return this;
    }

    public Integer getOobPort() {
        return oobPort;
    }

    public PhysicalServerProvisionTarget setOobPort(Integer oobPort) {
        this.oobPort = oobPort;
        return this;
    }

    public String getOobUsername() {
        return oobUsername;
    }

    public PhysicalServerProvisionTarget setOobUsername(String oobUsername) {
        this.oobUsername = oobUsername;
        return this;
    }

    public String getOobPassword() {
        return oobPassword;
    }

    public PhysicalServerProvisionTarget setOobPassword(String oobPassword) {
        this.oobPassword = oobPassword;
        return this;
    }

    public String getProvisionNicMac() {
        return provisionNicMac;
    }

    public PhysicalServerProvisionTarget setProvisionNicMac(String provisionNicMac) {
        this.provisionNicMac = provisionNicMac;
        return this;
    }

    public String getDhcpInterface() {
        return dhcpInterface;
    }

    public PhysicalServerProvisionTarget setDhcpInterface(String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
        return this;
    }

    public String getDhcpRangeStartIp() {
        return dhcpRangeStartIp;
    }

    public PhysicalServerProvisionTarget setDhcpRangeStartIp(String dhcpRangeStartIp) {
        this.dhcpRangeStartIp = dhcpRangeStartIp;
        return this;
    }

    public String getDhcpRangeEndIp() {
        return dhcpRangeEndIp;
    }

    public PhysicalServerProvisionTarget setDhcpRangeEndIp(String dhcpRangeEndIp) {
        this.dhcpRangeEndIp = dhcpRangeEndIp;
        return this;
    }

    public String getDhcpRangeNetmask() {
        return dhcpRangeNetmask;
    }

    public PhysicalServerProvisionTarget setDhcpRangeNetmask(String dhcpRangeNetmask) {
        this.dhcpRangeNetmask = dhcpRangeNetmask;
        return this;
    }

    public String getDhcpRangeGateway() {
        return dhcpRangeGateway;
    }

    public PhysicalServerProvisionTarget setDhcpRangeGateway(String dhcpRangeGateway) {
        this.dhcpRangeGateway = dhcpRangeGateway;
        return this;
    }

    public String getOsImageUuid() {
        return osImageUuid;
    }

    public PhysicalServerProvisionTarget setOsImageUuid(String osImageUuid) {
        this.osImageUuid = osImageUuid;
        return this;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public PhysicalServerProvisionTarget setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
        return this;
    }

    public String getKickstartTemplate() {
        return kickstartTemplate;
    }

    public PhysicalServerProvisionTarget setKickstartTemplate(String kickstartTemplate) {
        this.kickstartTemplate = kickstartTemplate;
        return this;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    public PhysicalServerProvisionTarget setCustomParams(Map<String, String> customParams) {
        this.customParams = customParams;
        return this;
    }
}
