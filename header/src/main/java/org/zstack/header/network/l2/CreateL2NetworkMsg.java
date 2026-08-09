package org.zstack.header.network.l2;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/** Internal, typed local L2 create request used by projection/recovery flows. */
public class CreateL2NetworkMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String zoneUuid;
    private String physicalInterface;
    private String type;
    private String vSwitchType = "LinuxBridge";
    private Boolean isolated = Boolean.FALSE;
    private String pvlan;
    private Integer vlan;
    private String resourceUuid;
    private String accountUuid;
    private NetworkCreateContext context;
    private SessionInventory session;
    private List<String> systemTags;

    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getZoneUuid() { return zoneUuid; }
    public void setZoneUuid(String value) { zoneUuid = value; }
    public String getPhysicalInterface() { return physicalInterface; }
    public void setPhysicalInterface(String value) { physicalInterface = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getvSwitchType() { return vSwitchType; }
    public void setvSwitchType(String value) { vSwitchType = value; }
    public Boolean getIsolated() { return isolated; }
    public void setIsolated(Boolean value) { isolated = value; }
    public String getPvlan() { return pvlan; }
    public void setPvlan(String value) { pvlan = value; }
    public Integer getVlan() { return vlan; }
    public void setVlan(Integer value) { vlan = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public String getAccountUuid() { return accountUuid; }
    public void setAccountUuid(String value) { accountUuid = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
    public SessionInventory getSession() { return session; }
    public void setSession(SessionInventory value) { session = value; }
    public List<String> getSystemTags() { return systemTags; }
    public void setSystemTags(List<String> value) { systemTags = value; }
}
