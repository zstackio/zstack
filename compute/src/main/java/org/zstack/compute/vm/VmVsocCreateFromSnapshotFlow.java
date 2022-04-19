package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVsocCreateFromSnapshotFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmVsocCreateFromSnapshotFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private List<VmBeforeCreateOnHypervisorExtensionPoint> exts;

    @Override
    public void run(FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        VmVsocCreateFromSnapshotMsg msg = new VmVsocCreateFromSnapshotMsg();
        msg.setVmUuid(spec.getVmInventory().getUuid());
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setSrcVmUuid(spec.getSrcVmUuid());
        msg.setPlatformId(CoreGlobalProperty.PLATFORM_ID);
        String scrVmRootVolumeUuid = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, spec.getSrcVmUuid()).select(VolumeVO_.uuid).findValue();
        msg.setSnapshotUuid(Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, scrVmRootVolumeUuid).select(VolumeSnapshotVO_.uuid).orderBy(VolumeVO_.createDate, SimpleQuery.Od.DESC).limit(1).findValue());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        DeleteVmVsocFileMsg msg = new DeleteVmVsocFileMsg();
        msg.setHostUuid(spec.getVmInventory().getHostUuid());
        msg.setPlatformId(CoreGlobalProperty.PLATFORM_ID);
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("Fail: vsoc_delete, because %s", reply.getError().getDetails()));
                }
                trigger.rollback();
            }
        });
    }
}
