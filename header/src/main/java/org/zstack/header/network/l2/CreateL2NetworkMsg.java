package org.zstack.header.network.l2;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.gson.JSONObjectUtil;

/** Internal, typed local L2 create request used by projection/recovery flows. */
public class CreateL2NetworkMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String zoneUuid;
    private String physicalInterface;
    private String type;
    private String vSwitchType = L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE;
    private Boolean isolated = Boolean.FALSE;
    private String pvlan;
    private Integer vlan;
    private String resourceUuid;
    private String accountUuid;
    private NetworkCreateContext context;
    private SessionInventory session;
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private Object subtypeMessage;
    private String subtypeMessageClassName;

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
    public APICreateL2NetworkMsg getSubtypeMessage() {
        if (subtypeMessage == null || subtypeMessage instanceof APICreateL2NetworkMsg) {
            return (APICreateL2NetworkMsg) subtypeMessage;
        }
        if (subtypeMessageClassName == null) {
            throw new IllegalArgumentException("missing l2 subtype message class");
        }

        try {
            Class<?> subtypeClass = Class.forName(subtypeMessageClassName);
            if (!APICreateL2NetworkMsg.class.isAssignableFrom(subtypeClass)) {
                throw new IllegalArgumentException(String.format(
                        "invalid l2 subtype message class[%s]", subtypeMessageClassName));
            }
            subtypeMessage = JSONObjectUtil.rehashObject(subtypeMessage, subtypeClass);
            return (APICreateL2NetworkMsg) subtypeMessage;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "cannot find l2 subtype message class[%s]", subtypeMessageClassName), e);
        }
    }

    public void setSubtypeMessage(APICreateL2NetworkMsg value) {
        subtypeMessage = value;
        subtypeMessageClassName = value == null ? null : value.getClass().getName();
    }
}
