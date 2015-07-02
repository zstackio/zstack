package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.IsoSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 5/23/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DownloadIsoForVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private CLogger logger = Utils.getLogger(DownloadIsoForVmExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(final VmInstanceSpec spec, final Completion completion) {
        final ImageInventory image = spec.getImageSpec().getInventory();
        if (image == null || image.getMediaType().equals(ImageMediaType.RootVolumeTemplate.toString()) || spec.getCurrentVmOperation() != VmOperation.NewCreate) {
            completion.success();
            return;
        }

        VolumeSpec vspec = spec.getVolumeSpecs().get(0);
        PrimaryStorageInventory pinv = vspec.getPrimaryStorageInventory();
        DownloadIsoToPrimaryStorageMsg msg = new DownloadIsoToPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(pinv.getUuid());
        msg.setIsoSpec(spec.getImageSpec());
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setDestHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pinv.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    DownloadIsoToPrimaryStorageReply re = reply.castReply();
                    IsoSpec iso = new IsoSpec();
                    iso.setInstallPath(re.getInstallPath());
                    iso.setImageUuid(image.getUuid());
                    spec.setDestIso(iso);
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, final Completion completion) {
        if (spec.getDestIso() != null) {
            VolumeSpec vspec = spec.getVolumeSpecs().get(0);
            PrimaryStorageInventory pinv = vspec.getPrimaryStorageInventory();
            DeleteIsoFromPrimaryStorageMsg msg = new DeleteIsoFromPrimaryStorageMsg();
            msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
            msg.setIsoSpec(spec.getImageSpec());
            msg.setPrimaryStorageUuid(pinv.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pinv.getUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }
                    completion.success();
                }
            });
        } else {
            completion.success();
        }
    }
}
