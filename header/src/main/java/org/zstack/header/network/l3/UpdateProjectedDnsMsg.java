package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.NetworkCreateContext;

import java.util.ArrayList;
import java.util.List;

/** Internal typed DNS projection request. */
public class UpdateProjectedDnsMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private List<String> dns = new ArrayList<>();
    private NetworkCreateContext context;

    @Override
    public String getL3NetworkUuid() { return l3NetworkUuid; }
    public void setL3NetworkUuid(String value) { l3NetworkUuid = value; }
    public List<String> getDns() { return dns; }
    public void setDns(List<String> value) { dns = value; }
    public NetworkCreateContext getContext() { return context; }
    public void setContext(NetworkCreateContext value) { context = value; }
}
