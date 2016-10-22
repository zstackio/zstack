package org.zstack.appliancevm;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmSpec implements Serializable {
    private ApplianceVmNicSpec managementNic;
    private List<ApplianceVmNicSpec> additionalNics;
    private InstanceOfferingInventory instanceOffering;
    private List<DiskOfferingInventory> dataDisk;
    private ImageInventory template;
    private String name;
    private String uuid;
    private String description;
    private L3NetworkInventory defaultRouteL3Network;
    private ApplianceVmType applianceVmType;
    private String accountUuid;
    private boolean syncCreate;
    private List<ApplianceVmFirewallRuleInventory> firewallRules;
    private List<String> inherentSystemTags;
    private List<String> nonInherentSystemTags;
    private String sshUsername = "root";
    private int sshPort = 22;
    private int agentPort = 7759;

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public List<String> getNonInherentSystemTags() {
        return nonInherentSystemTags;
    }

    public void setNonInherentSystemTags(List<String> nonInherentSystemTags) {
        this.nonInherentSystemTags = nonInherentSystemTags;
    }

    public List<String> getInherentSystemTags() {
        return inherentSystemTags;
    }

    public void setInherentSystemTags(List<String> inherentSystemTags) {
        this.inherentSystemTags = inherentSystemTags;
    }

    public List<ApplianceVmFirewallRuleInventory> getFirewallRules() {
        if (firewallRules == null) {
            firewallRules = new ArrayList<ApplianceVmFirewallRuleInventory>();
        }
        return firewallRules;
    }

    public void setFirewallRules(List<ApplianceVmFirewallRuleInventory> firewallRules) {
        this.firewallRules = firewallRules;
    }

    public ApplianceVmNicSpec getManagementNic() {
        return managementNic;
    }

    public void setManagementNic(ApplianceVmNicSpec managementNic) {
        this.managementNic = managementNic;
    }

    public List<ApplianceVmNicSpec> getAdditionalNics() {
        if (additionalNics == null) {
            additionalNics = new ArrayList<ApplianceVmNicSpec>();
        }
        return additionalNics;
    }

    public void setAdditionalNics(List<ApplianceVmNicSpec> additionalNics) {
        this.additionalNics = additionalNics;
    }

    public InstanceOfferingInventory getInstanceOffering() {
        return instanceOffering;
    }

    public void setInstanceOffering(InstanceOfferingInventory instanceOffering) {
        this.instanceOffering = instanceOffering;
    }

    public List<DiskOfferingInventory> getDataDisk() {
        return dataDisk;
    }

    public void setDataDisk(List<DiskOfferingInventory> dataDisk) {
        this.dataDisk = dataDisk;
    }

    public ImageInventory getTemplate() {
        return template;
    }

    public void setTemplate(ImageInventory template) {
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public L3NetworkInventory getDefaultRouteL3Network() {
        return defaultRouteL3Network;
    }

    public void setDefaultRouteL3Network(L3NetworkInventory defaultRouteL3Network) {
        this.defaultRouteL3Network = defaultRouteL3Network;
    }

    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(ApplianceVmType applianceVmType) {
        this.applianceVmType = applianceVmType;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isSyncCreate() {
        return syncCreate;
    }

    public void setSyncCreate(boolean syncCreate) {
        this.syncCreate = syncCreate;
    }
}
