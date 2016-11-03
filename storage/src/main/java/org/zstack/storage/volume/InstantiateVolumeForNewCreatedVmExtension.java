package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Od;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class InstantiateVolumeForNewCreatedVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVolumeForNewCreatedVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException{
    }

    private void doInstantiate(final Iterator<InstantiateVolumeMsg> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        InstantiateVolumeMsg msg  = it.next();
        bus.send(msg, new CloudBusCallBack(completion) {
            private int getNextDeviceId() {
                SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                q.select(VolumeVO_.deviceId);
                q.add(VolumeVO_.vmInstanceUuid, Op.EQ, spec.getVmInventory().getUuid());
                q.add(VolumeVO_.deviceId, Op.NOT_NULL);
                q.orderBy(VolumeVO_.deviceId, Od.ASC);
                List<Integer> devIds = q.listValue();

                BitSet full = new BitSet(devIds.size()+1);
                for (Integer id : devIds) {
                    full.set(id);
                }
                return full.nextClearBit(0);
            }

            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    InstantiateVolumeReply r = reply.castReply();
                    VolumeVO vo = dbf.findByUuid(r.getVolume().getUuid(), VolumeVO.class);
                    if (vo.getType() == VolumeType.Data) {
                        vo.setDeviceId(getNextDeviceId());
                        vo.setActualSize(0L);
                    } else {
                        vo.setActualSize(spec.getImageSpec().getInventory().getActualSize());
                    }
                    vo = dbf.updateAndRefresh(vo);

                    VolumeInventory vinv = VolumeInventory.valueOf(vo);
                    if (spec.getDestRootVolume().getUuid().equals(vinv.getUuid())) {
                        spec.setDestRootVolume(vinv);
                    } else {
                        spec.getDestDataVolumes().add(vinv);
                    }

                    logger.debug(String.format("spec.getDestRootVolume is: %s", spec.getDestRootVolume().getInstallPath()));
                    logger.debug(String.format("successfully instantiated volume%s", JSONObjectUtil.toJsonString(vinv)));

                    doInstantiate(it, spec, completion);
                } else {
                    completion.fail(reply.getError());
                }

            }
        });
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (!spec.getVmInventory().getState().equals(VmInstanceState.Created.toString())) {
            completion.success();
            return;
        }

        List<InstantiateVolumeMsg> msgs = new ArrayList<>();
        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
            msg.setVolumeUuid(volume.getUuid());
            msg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
            msg.setHostUuid(spec.getDestHost().getUuid());
            msg.setPrimaryStorageAllocated(true);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
            msgs.add(msg);
        }

        ImageSpec image = spec.getImageSpec();
        InstantiateVolumeMsg rmsg;
        if (ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            rmsg = new InstantiateRootVolumeMsg();
            ((InstantiateRootVolumeMsg)rmsg).setTemplateSpec(image);
        } else {
            rmsg = new InstantiateVolumeMsg();
        }
        rmsg.setPrimaryStorageUuid(spec.getDestRootVolume().getPrimaryStorageUuid());
        rmsg.setHostUuid(spec.getDestHost().getUuid());
        rmsg.setVolumeUuid(spec.getDestRootVolume().getUuid());
        rmsg.setPrimaryStorageAllocated(true);
        bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeConstant.SERVICE_ID, spec.getDestRootVolume().getUuid());
        msgs.add(rmsg);

        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        // data volume will be refilled after being instantiated
        spec.getDestDataVolumes().clear();
        doInstantiate(msgs.iterator(), spec, completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        /* volumes will be deleted when VmAllocateVolumeFlow rolls back */
        completion.success();
    }
}
