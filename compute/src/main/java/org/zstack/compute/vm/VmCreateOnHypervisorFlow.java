package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCreateOnHypervisorFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmCreateOnHypervisorFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    private static String SUCCESS = "VmCreateOnHypervisorFlow.success";

    private List<VmBeforeCreateOnHypervisorExtensionPoint> exts;

    public VmCreateOnHypervisorFlow() {
        exts = pluginRgty.getExtensionList(VmBeforeCreateOnHypervisorExtensionPoint.class);
    }

    private void fireExtensions(VmInstanceSpec spec) {
        for (VmBeforeCreateOnHypervisorExtensionPoint ext : exts) {
            ext.beforeCreateVmOnHypervisor(spec);
        }
    }

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        taskProgress("start on the hypervisor");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        fireExtensions(spec);

        CreateVmOnHypervisorMsg msg = new CreateVmOnHypervisorMsg();
        msg.setVmSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    data.put(SUCCESS, true);
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        if (!data.containsKey(SUCCESS)) {
            trigger.rollback();
            return;
        }

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        DestroyVmOnHypervisorMsg msg = new DestroyVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        msg.getVmInventory().setHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to roll back vm[uuid:%s, name:%s] on host[uuid:%s, ip:%s], %s",
                            spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                            spec.getDestHost().getUuid(), spec.getDestHost().getName(), reply.getError()));
                }
                trigger.rollback();
            }
        });
    }
}
