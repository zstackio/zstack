package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;

import java.util.List;

/**
 * Created by xing5 on 2017/3/6.
 *
 * Retained only to drain pre-upgrade GarbageCollectorVO records without
 * ClassNotFoundException; FlatEipBackend reconciles host EIP state via /eip/sync.
 */
@Deprecated
public class FlatEipGC extends EventBasedGarbageCollector {
    @Autowired
    private FlatEipBackend eipBackend;

    @GC
    public List<FlatEipBackend.EipTO> eips;
    @GC
    public String hostUuid;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (hostUuid != null && dbf.isExist(hostUuid, HostVO.class) && eipBackend != null) {
            eipBackend.markHostEipDirty(hostUuid);
        }
        completion.cancel();
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return hostUuid != null && hostUuid.equals(d.getHostUuid()) && d.getNewStatus().equals(HostStatus.Connected.toString());
        }));

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid != null && hostUuid.equals(d.getHostUuid());
        }));
    }
}
