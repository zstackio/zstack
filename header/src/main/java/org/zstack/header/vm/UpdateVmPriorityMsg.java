package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 20:13 2019/9/18
 */
public class UpdateVmPriorityMsg extends NeedReplyMessage implements HostMessage {

    // vmUuid, VmPriorityLevel
    private Map<String, VmPriorityLevel> vmlevelMap;

    private String hostUuid;

    public Map<String, VmPriorityLevel> getVmlevelMap() {
        return vmlevelMap;
    }

    public void setVmlevelMap(Map<String, VmPriorityLevel> vmlevelMap) {
        this.vmlevelMap = vmlevelMap;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}