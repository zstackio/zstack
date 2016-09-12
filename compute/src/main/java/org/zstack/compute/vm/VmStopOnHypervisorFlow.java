package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.EventBasedGCPersistentContext;
import org.zstack.core.gc.GCEventTrigger;
import org.zstack.core.gc.GCFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;

import java.util.Map;

import static org.zstack.utils.StringDSL.ln;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmStopOnHypervisorFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected GCFacade gcf;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        StopVmOnHypervisorMsg msg = new StopVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        msg.setForce(((APIStopVmInstanceMsg)spec.getMessage()).getForce());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getVmInventory().getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                } else {
                    if (spec.isGcOnStopFailure() && reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                        setupGcJob(spec);
                        chain.next();
                    } else {
                        chain.fail(reply.getError());
                    }
                }
            }
        });
    }

    private void setupGcJob(VmInstanceSpec spec) {
        GCStopVmContext c = new GCStopVmContext();
        VmInstanceInventory vm = spec.getVmInventory();
        c.setHostUuid(vm.getHostUuid());
        c.setVmUuid(vm.getUuid());
        c.setInventory(vm);
        c.setTriggerHostStatus(HostStatus.Connected.toString());

        EventBasedGCPersistentContext<GCStopVmContext> ctx = new EventBasedGCPersistentContext<GCStopVmContext>();
        ctx.setRunnerClass(GCStopVmRunner.class);
        ctx.setContextClass(GCStopVmContext.class);
        ctx.setName(String.format("stop-vm-%s", spec.getVmInventory().getUuid()));
        ctx.setContext(c);

        GCEventTrigger trigger = new GCEventTrigger();
        trigger.setCodeName("gc-stop-vm-on-host-connected");
        trigger.setEventPath(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH);
        String code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData",
                "import org.zstack.compute.vm.GCStopVmContext",
                "HostStatusChangedData d = (HostStatusChangedData) data",
                "GCStopVmContext c = (GCStopVmContext) context",
                "return c.hostUuid == d.hostUuid && d.newStatus == c.triggerHostStatus"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        trigger = new GCEventTrigger();
        trigger.setCodeName("gc-stop-vm-on-host-deleted");
        trigger.setEventPath(HostCanonicalEvents.HOST_DELETED_PATH);
        code = ln(
                "import org.zstack.header.host.HostCanonicalEvents.HostDeletedData",
                "import org.zstack.compute.vm.GCStopVmContext",
                "HostDeletedData d = (HostDeletedData) data",
                "GCStopVmContext c = (GCStopVmContext) context",
                "return c.hostUuid == d.hostUuid"
        ).toString();
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        trigger = new GCEventTrigger();
        trigger.setCodeName("gc-stop-vm-on-vm-deleted");
        trigger.setEventPath(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH);
        code = ln(
                "import org.zstack.header.vm.VmCanonicalEvents.VmStateChangedData",
                "import org.zstack.compute.vm.GCStopVmContext",
                "VmStateChangedData d = (VmStateChangedData) data",
                "GCStopVmContext c = (GCStopVmContext) context",
                "return c.vmUuid == d.vmUuid && d.newState == \"{0}\""
        ).format(VmInstanceState.Destroyed.toString());
        trigger.setCode(code);
        ctx.addTrigger(trigger);

        gcf.schedule(ctx);
    }
}
