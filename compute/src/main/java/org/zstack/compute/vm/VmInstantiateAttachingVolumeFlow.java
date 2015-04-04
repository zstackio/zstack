package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.Flow;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.InstantiateVolumeMsg;
import org.zstack.header.storage.primary.InstantiateVolumeReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateAttachingVolumeFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(final FlowTrigger chain, final Map ctx) {
        final VolumeInventory volume = (VolumeInventory) ctx.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        assert volume != null;
        assert spec != null;

        final PrimaryStorageInventory pinv = (PrimaryStorageInventory) ctx.get(VmInstanceConstant.Params.DestPrimaryStorageInventoryForAttachingVolume.toString());
        volume.setPrimaryStorageUuid(pinv.getUuid());
        InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
        msg.setDestHost(spec.getDestHost());
        msg.setVolume(volume);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pinv.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    InstantiateVolumeReply r = (InstantiateVolumeReply) reply;
                    VolumeVO vo = dbf.findByUuid(r.getVolume().getUuid(), VolumeVO.class);
                    vo.setPrimaryStorageUuid(pinv.getUuid());
                    vo.setInstallPath(r.getVolume().getInstallPath());
                    vo.setStatus(VolumeStatus.Ready);
                    vo = dbf.updateAndRefresh(vo);
                    ctx.put(VmInstanceConstant.Params.AttachingVolumeInventory.toString(), VolumeInventory.valueOf(vo));
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowTrigger chain, Map data) {
        final VolumeInventory volume = (VolumeInventory) data.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        VolumeVO vo = dbf.findByUuid(volume.getUuid(), VolumeVO.class);
        vo.setPrimaryStorageUuid(null);
        vo.setInstallPath(null);
        vo.setStatus(VolumeStatus.NotInstantiated);
        dbf.update(vo);
        chain.rollback();
    }
}
