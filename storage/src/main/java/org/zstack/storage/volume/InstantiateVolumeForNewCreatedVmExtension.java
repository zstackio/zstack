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
import org.zstack.header.storage.primary.InstantiateRootVolumeFromTemplateMsg;
import org.zstack.header.storage.primary.InstantiateVolumeMsg;
import org.zstack.header.storage.primary.InstantiateVolumeReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
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

    private List<InstantiateVolumeMsg> prepareMsg(VmInstanceSpec spec) {
        List<InstantiateVolumeMsg> msgs = new ArrayList<InstantiateVolumeMsg>();
        for (VolumeInventory volume : spec.getDestDataVolumes()) {
            InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
            msg.setVolume(volume);
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
            msg.setDestHost(spec.getDestHost());
            msgs.add(msg);
        }

        ImageSpec image = spec.getImageSpec();
        InstantiateVolumeMsg rmsg =  null;
        if (ImageMediaType.RootVolumeTemplate.toString().equals(image.getInventory().getMediaType())) {
            rmsg = new InstantiateRootVolumeFromTemplateMsg();
            ((InstantiateRootVolumeFromTemplateMsg)rmsg).setTemplateSpec(image);
        } else {
            rmsg = new InstantiateVolumeMsg();
        }
        rmsg.setDestHost(spec.getDestHost());
        rmsg.setVolume(spec.getDestRootVolume());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, spec.getDestRootVolume().getPrimaryStorageUuid());
        msgs.add(rmsg);
        return msgs;
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
                    InstantiateVolumeReply r = (InstantiateVolumeReply) reply;
                    VolumeVO vo = dbf.findByUuid(r.getVolume().getUuid(), VolumeVO.class);

                    VolumeStatus oldStatus = vo.getStatus();

                    vo.setInstallPath(r.getVolume().getInstallPath());
                    vo.setStatus(VolumeStatus.Ready);
                    if (vo.getType() == VolumeType.Data) {
                        vo.setDeviceId(getNextDeviceId());
                        vo.setActualSize(0L);
                    } else {
                        vo.setActualSize(spec.getImageSpec().getInventory().getActualSize());
                    }
                    vo = dbf.updateAndRefresh(vo);

                    new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, VolumeInventory.valueOf(vo));

                    VolumeInventory vinv = VolumeInventory.valueOf(vo);
                    if (spec.getDestRootVolume().getUuid().equals(vinv.getUuid())) {
                        spec.setDestRootVolume(vinv);
                    } else {
                        spec.getDestDataVolumes().add(vinv);
                    }

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

        List<InstantiateVolumeMsg> msgs = prepareMsg(spec);
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
