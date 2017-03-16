package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/3.
 */
public class DeleteVmGC extends EventBasedGarbageCollector {
    @GC
    public String hostUuid;
    @GC
    public VmInstanceInventory inventory;

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid.equals(d.getHostUuid());
        });

        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        });
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        HostStatus status = Q.New(HostVO.class).select(HostVO_.status)
                .eq(HostVO_.uuid, hostUuid).findValue();
        if (status == null) {
            // the host has been deleted
            completion.cancel();
            return;
        }

        if (status != HostStatus.Connected) {
            completion.fail(operr("the host[uuid:%s] is not connected", hostUuid));
            return;
        }

        VmInstanceState vmstate = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, inventory.getUuid()).findValue();

        if (vmstate != null && vmstate != VmInstanceState.Destroyed) {
            // the vm has been recovered
            completion.cancel();
            return;
        }

        VmDirectlyDestroyOnHypervisorMsg msg = new VmDirectlyDestroyOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
