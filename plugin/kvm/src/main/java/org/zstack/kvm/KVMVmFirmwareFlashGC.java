package org.zstack.kvm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;

public class KVMVmFirmwareFlashGC extends TimeBasedGarbageCollector {
    @GC
    public String vmUuid;
    @GC
    public String hostUuid;
    @GC
    public String generation;

    @Override
    protected void triggerNow(GCCompletion completion) {
        KVMVmFirmwareFlashCleanupMsg msg = new KVMVmFirmwareFlashCleanupMsg();
        msg.setVmUuid(vmUuid);
        msg.setHostUuid(hostUuid);
        msg.setGeneration(generation);
        msg.setJobUuid(getUuid());
        // Route to the VM owner so HA-reloaded GC jobs use the same VM operation chain.
        bus.makeTargetServiceIdByResourceUuid(msg, KVMVmFirmwareFlashCleanup.SERVICE_ID, vmUuid);
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
