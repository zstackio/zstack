package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.EventBasedGCPersistentContext;
import org.zstack.core.gc.GCEventTrigger;
import org.zstack.core.gc.GCFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.ExpungeVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 11/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeRootVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeRootVolumeFlow.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected GCFacade gcf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getRootVolumeUuid() == null) {
            // the vm is in an intermediate state that has no root volume
            trigger.next();
            return;
        }

        ExpungeVolumeMsg msg = new ExpungeVolumeMsg();
        msg.setVolumeUuid(spec.getVmInventory().getRootVolumeUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    trigger.next();
                    return;
                }

                if (!reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    logger.warn(String.format("failed to expunge the root volume[uuid:%s] of the vm[uuid:%s, name:%s], %s",
                            spec.getVmInventory().getRootVolumeUuid(), spec.getVmInventory().getUuid(),
                            spec.getVmInventory().getName(), reply.getError()));
                    trigger.fail(reply.getError());

                    return;
                }

                prepareGcJob(spec.getVmInventory());
                trigger.next();
            }
        });
    }

    private void prepareGcJob(final VmInstanceInventory vmInv) {
        final String hostUuid = vmInv.getHostUuid() == null ? vmInv.getLastHostUuid() : vmInv.getHostUuid();
        GCExpungeVmContext c = new GCExpungeVmContext();
        c.setHostUuid(hostUuid);
        c.setVmUuid(vmInv.getUuid());
        c.setInventory(vmInv);
        c.setTriggerHostStatus(HostStatus.Connected.toString());

        EventBasedGCPersistentContext<GCExpungeVmContext> ctx = new EventBasedGCPersistentContext<GCExpungeVmContext>();
        ctx.setRunnerClass(GCExpungeVmRunner.class);
        ctx.setContextClass(GCExpungeVmContext.class);
        ctx.setName(String.format("expunge-vm-%s", vmInv.getUuid()));
        ctx.setContext(c);

        GCEventTrigger trigger = new GCEventTrigger();
        trigger.setCodeName("gc-expunge-vm-on-host-connected");
        trigger.setEventPath(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH);
        String code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData",
                "import org.zstack.compute.vm.GCExpungeVmContext",
                "HostStatusChangedData d = (HostStatusChangedData) data",
                "GCExpungeVmContext c = (GCExpungeVmContext) context",
                "return c.hostUuid == d.hostUuid && d.newStatus == c.triggerHostStatus"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        trigger = new GCEventTrigger();
        trigger.setCodeName("gc-delete-vm-on-host-deleted");
        trigger.setEventPath(HostCanonicalEvents.HOST_DELETED_PATH);
        code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostDeletedData",
                "import org.zstack.compute.vm.GCExpungeVmContext",
                "HostDeletedData d = (HostDeletedData) data",
                "GCExpungeVmContext c = (GCExpungeVmContext) context",
                "return c.hostUuid == d.hostUuid"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        gcf.schedule(ctx);
    }
}
