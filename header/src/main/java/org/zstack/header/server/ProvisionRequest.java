package org.zstack.header.server;

import java.io.Serializable;
import java.util.Map;

public class ProvisionRequest implements Serializable {
    private String serverUuid;
    private String networkUuid;
    private ProvisionPhase startPhase = ProvisionPhase.NotStarted;
    private String osImageUuid;
    private String osDistribution;
    private String kickstartTemplate;
    private String provisionNicMac;
    private Map<String, String> customParams;
    private String accountUuid;
    private PhysicalServerProvisionTarget target;

    public String getServerUuid() {
        return serverUuid;
    }

    public ProvisionRequest setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
        return this;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public ProvisionRequest setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
        return this;
    }

    public String getOsImageUuid() {
        return osImageUuid;
    }

    public ProvisionRequest setOsImageUuid(String osImageUuid) {
        this.osImageUuid = osImageUuid;
        return this;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public ProvisionRequest setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
        return this;
    }

    public String getKickstartTemplate() {
        return kickstartTemplate;
    }

    public ProvisionRequest setKickstartTemplate(String kickstartTemplate) {
        this.kickstartTemplate = kickstartTemplate;
        return this;
    }

    public String getProvisionNicMac() {
        return provisionNicMac;
    }

    public ProvisionRequest setProvisionNicMac(String provisionNicMac) {
        this.provisionNicMac = provisionNicMac;
        return this;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    public ProvisionRequest setCustomParams(Map<String, String> customParams) {
        this.customParams = customParams;
        return this;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public ProvisionRequest setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
        return this;
    }

    public PhysicalServerProvisionTarget getTarget() {
        return target;
    }

    public ProvisionRequest setTarget(PhysicalServerProvisionTarget target) {
        this.target = target;
        return this;
    }

    public ProvisionPhase getStartPhase() {
        return startPhase;
    }

    public ProvisionRequest setStartPhase(ProvisionPhase startPhase) {
        this.startPhase = startPhase;
        return this;
    }
}
