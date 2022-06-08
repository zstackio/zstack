package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.devices.VmInstanceDeviceManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAttachVolumeOnHypervisorFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VmInstanceDeviceManager vidm;
    
    @Override
    public void run(final FlowTrigger chain, Map ctx) {
        final VolumeInventory volume = (VolumeInventory) ctx.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final List<VolumeInventory> attachedDataVolumes = (List<VolumeInventory>) ctx.get(VmInstanceConstant.Params.AttachedDataVolumeInventories.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        assert volume != null;
        assert spec != null;
        assert attachedDataVolumes != null;

        String vmState = spec.getVmInventory().getState();
        if (VmInstanceState.Stopped.toString().equals(vmState)) {
            chain.next();
            return;
        }

        assert VmInstanceState.Running.toString().equals(vmState) || VmInstanceState.Paused.toString().equals(vmState);

        String hostUuid = spec.getVmInventory().getHostUuid();

        AttachVolumeToVmOnHypervisorMsg msg = new AttachVolumeToVmOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmInventory(spec.getVmInventory());
        msg.setInventory(volume);
        msg.setAttachedDataVolumes(attachedDataVolumes);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    processDeviceAddressInfo(reply.castReply(), spec);

                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    private void processDeviceAddressInfo(AttachVolumeToVmOnHypervisorReply reply, final VmInstanceSpec spec) {
        if (reply.getVirtualDeviceInfoList() == null || reply.getVirtualDeviceInfoList().isEmpty()) {
            return;
        }

        for (VirtualDeviceInfo info : reply.getVirtualDeviceInfoList()) {
            vidm.createOrUpdateVmDeviceAddress(info, spec.getVmInventory().getUuid());
        }
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
