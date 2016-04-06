package org.zstack.network.service.flat;

import org.zstack.network.service.flat.FlatEipBackend.EipTO;

import java.util.List;

/**
 * Created by xing5 on 2016/4/6.
 */
public class GCDeleteEipContext {
    private List<EipTO> eips;
    private String hostUuid;
    private String triggerHostStatus;

    public String getTriggerHostStatus() {
        return triggerHostStatus;
    }

    public void setTriggerHostStatus(String triggerHostStatus) {
        this.triggerHostStatus = triggerHostStatus;
    }

    public List<EipTO> getEips() {
        return eips;
    }

    public void setEips(List<EipTO> eips) {
        this.eips = eips;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
