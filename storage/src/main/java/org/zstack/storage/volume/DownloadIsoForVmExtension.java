package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.CdRomSpec;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 5/23/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DownloadIsoForVmExtension implements PreVmInstantiateResourceExtensionPoint {
    private CLogger logger = Utils.getLogger(DownloadIsoForVmExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static List<VmOperation> operations;
    static {
        operations = list(
                VmOperation.NewCreate,
                VmOperation.Start,
                VmOperation.Reboot
        );
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    }

    @Override
    public void preInstantiateVmResource(final VmInstanceSpec spec, final Completion completion) {
        if (spec.getCdRomSpecs().isEmpty() || !operations.contains(spec.getCurrentVmOperation())) {
            completion.success();
            return;
        }

        List<String> isoUuids = spec.getCdRomSpecs().stream().map(CdRomSpec::getImageUuid).collect(Collectors.toList());
        if (isoUuids.isEmpty()) {
            completion.success();
            return;
        }

        spec.getCdRomSpecs().forEach(cdRomSpec -> {
            if (cdRomSpec.getImageUuid() == null) {
                return;
            }
            assert cdRomSpec.getBackupStorageUuid() != null : "backup storage uuid cannot be null";
        });

        List<DownloadIsoToPrimaryStorageMsg> msgs = CollectionUtils.transformToList(spec.getCdRomSpecs(),
             new Function<DownloadIsoToPrimaryStorageMsg, CdRomSpec>() {
                @Override
                public DownloadIsoToPrimaryStorageMsg call(CdRomSpec cdRomSpec) {
                    if (cdRomSpec.getImageUuid() == null) {
                        return null;
                    }

                    final String psUuid;
                    ImageSpec imageSpec = new ImageSpec();
                    final ImageInventory iso = ImageInventory.valueOf(dbf.findByUuid(cdRomSpec.getImageUuid(), ImageVO.class));
                    imageSpec.setInventory(iso);
                    imageSpec.setSelectedBackupStorage(CollectionUtils.find(iso.getBackupStorageRefs(), new Function<ImageBackupStorageRefInventory, ImageBackupStorageRefInventory>() {
                        @Override
                        public ImageBackupStorageRefInventory call(ImageBackupStorageRefInventory arg) {
                            return arg.getBackupStorageUuid().equals(cdRomSpec.getBackupStorageUuid()) &&
                                    ImageStatus.Ready.toString().equals(arg.getStatus())? arg : null;
                        }
                    }));

                    if (VmOperation.NewCreate == spec.getCurrentVmOperation()) {
                        VolumeSpec vspec = spec.getVolumeSpecs().get(0);
                        PrimaryStorageInventory pinv = vspec.getPrimaryStorageInventory();
                        psUuid = pinv.getUuid();
                    } else {
                        psUuid = spec.getVmInventory().getRootVolume().getPrimaryStorageUuid();
                    }

                    DownloadIsoToPrimaryStorageMsg msg = new DownloadIsoToPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(psUuid);
                    msg.setIsoSpec(imageSpec);
                    msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
                    msg.setDestHostUuid(spec.getDestHost().getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
                    return msg;
                }
             }
        );

        List<ErrorCode> errorCodes = new ArrayList<>();
        new While<>(msgs).all((msg, whileCompletion) -> {
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        DownloadIsoToPrimaryStorageReply re = reply.castReply();
                        Optional<CdRomSpec> optional = spec.getCdRomSpecs().stream()
                                .filter(s -> s.getImageUuid() != null && s.getImageUuid().equals(msg.getIsoSpec().getInventory().getUuid()))
                                .findAny();
                        if (optional.isPresent()) {
                            CdRomSpec cdRomSpec = optional.get();
                            cdRomSpec.setInstallPath(re.getInstallPath());
                            cdRomSpec.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());

                            SQL.New(VmCdRomVO.class)
                                    .eq(VmCdRomVO_.uuid, cdRomSpec.getUuid())
                                    .set(VmCdRomVO_.isoInstallPath, re.getInstallPath())
                                    .update();
                        }
                    } else {
                        errorCodes.add(reply.getError());
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodes.isEmpty()) {
                    completion.success();
                    return;
                }

                ErrorCode ec = operr(new ErrorCodeList().causedBy(errorCodes), "unable to download iso to primary storage, becasue: %s",
                        errorCodes.get(0).getDetails());

                completion.fail(ec);
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, final Completion completion) {
        List<CdRomSpec> cdRomSpecs = spec.getCdRomSpecs();

        if (cdRomSpecs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> isoUuids = spec.getCdRomSpecs().stream().map(CdRomSpec::getImageUuid).collect(Collectors.toList());
        isoUuids = isoUuids.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
        if (isoUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<DeleteIsoFromPrimaryStorageMsg> msgs = CollectionUtils.transformToList(cdRomSpecs,
                new Function<DeleteIsoFromPrimaryStorageMsg, CdRomSpec>() {
                    @Override
                    public DeleteIsoFromPrimaryStorageMsg call(CdRomSpec arg) {
                        if (arg.getImageUuid() == null) {
                            return null;
                        }

                        String psUuid;
                        if (VmOperation.NewCreate == spec.getCurrentVmOperation()) {
                            VolumeSpec vspec = spec.getVolumeSpecs().get(0);
                            PrimaryStorageInventory pinv = vspec.getPrimaryStorageInventory();
                            psUuid = pinv.getUuid();
                        } else {
                            psUuid = spec.getVmInventory().getRootVolume().getPrimaryStorageUuid();
                        }

                        DeleteIsoFromPrimaryStorageMsg msg = new DeleteIsoFromPrimaryStorageMsg();
                        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
                        msg.setIsoSpec(spec.getImageSpec());
                        msg.setPrimaryStorageUuid(psUuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
                        return msg;
                    }
                }
        );

        new While<>(msgs).all((msg, whileCompletion) -> {
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
