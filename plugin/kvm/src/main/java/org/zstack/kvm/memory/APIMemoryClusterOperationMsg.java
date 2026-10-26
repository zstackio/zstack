package org.zstack.kvm.memory;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/** Shared request of the Cluster pause, resume and drain APIs; hostUuid limits it to one Host. */
public abstract class APIMemoryClusterOperationMsg extends APICreateMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String uuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
}
