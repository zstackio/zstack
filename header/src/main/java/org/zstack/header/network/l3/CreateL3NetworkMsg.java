package org.zstack.header.network.l3;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

/** Internal typed L3 create request for ZNS projection and recovery. */
public class CreateL3NetworkMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE;
    private String l2NetworkUuid;
    private String category = L3NetworkCategory.Private.toString();
    private Integer ipVersion;
    private boolean system;
    private String dnsDomain;
    private Boolean enableIPAM = Boolean.TRUE;
    private String resourceUuid;
    private SessionInventory session;
    private NetworkCreateContext context;

    public static CreateL3NetworkMsg fromApi(APICreateL3NetworkMsg api,
                                             NetworkCreateContext context) {
        CreateL3NetworkMsg msg = new CreateL3NetworkMsg();
        msg.setName(api.getName());
        msg.setDescription(api.getDescription());
        msg.setType(api.getType());
        msg.setL2NetworkUuid(api.getL2NetworkUuid());
        msg.setCategory(api.getCategory());
        msg.setIpVersion(api.getIpVersion());
        msg.setSystem(api.isSystem());
        msg.setDnsDomain(api.getDnsDomain());
        msg.setEnableIPAM(api.getEnableIPAM());
        msg.setResourceUuid(api.getResourceUuid());
        msg.setSession(api.getSession());
        msg.setSystemTags(api.getSystemTags());
        msg.setUserTags(api.getUserTags());
        msg.setContext(context);
        msg.setId(api.getId());
        return msg;
    }

    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getL2NetworkUuid() { return l2NetworkUuid; }
    public void setL2NetworkUuid(String value) { l2NetworkUuid = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public Integer getIpVersion() { return ipVersion; }
    public void setIpVersion(Integer value) { ipVersion = value; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean value) { system = value; }
    public String getDnsDomain() { return dnsDomain; }
    public void setDnsDomain(String value) { dnsDomain = value; }
    public Boolean getEnableIPAM() { return enableIPAM; }
    public void setEnableIPAM(Boolean value) { enableIPAM = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public SessionInventory getSession() { return session; }
    public void setSession(SessionInventory value) { session = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
