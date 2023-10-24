package org.zstack.storage.addon.primary;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.NvmeRemoteTarget;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ExternalPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorage.class);

    private final PrimaryStorageNodeSvc node;
    private final PrimaryStorageControllerSvc controller;

    private ExternalPrimaryStorageVO externalVO;
    private LinkedHashMap selfConfig;

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager acntMgr;


    public ExternalPrimaryStorage(PrimaryStorageVO self, PrimaryStorageControllerSvc controller, PrimaryStorageNodeSvc node) {
        super(self);
        this.controller = controller;
        this.node = node;
        this.externalVO = Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                .find();
        this.selfConfig = JSONObjectUtil.toObject(externalVO.getConfig(), LinkedHashMap.class);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateExternalPrimaryStorageMsg) {
            handle((APIUpdateExternalPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateExternalPrimaryStorageMsg msg) {
        APIUpdateExternalPrimaryStorageEvent evt = new APIUpdateExternalPrimaryStorageEvent(msg.getId());
        if (msg.getName() != null) {
            externalVO.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            externalVO.setDescription(msg.getDescription());
        }
        if (msg.getUrl() != null) {
            externalVO.setUrl(msg.getUrl());
        }
        if (msg.getDefaultProtocol() != null) {
            externalVO.setDefaultProtocol(msg.getDefaultProtocol());
        }
        if (msg.getConfig() != null) {
            controller.validateConfig(msg.getConfig());
            externalVO.setConfig(msg.getConfig());
        }
        externalVO = dbf.updateAndRefresh(externalVO);
        evt.setInventory(externalVO.toInventory());
        bus.publish(evt);
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) {
            throw new UnsupportedOperationException();
        } else if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else if (msg instanceof InstantiateTemporaryVolumeOnPrimaryStorageMsg) {
            throw new UnsupportedOperationException();
        } else if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            throw new UnsupportedOperationException();
        } else {
            createEmptyVolume(msg);
        }
    }

    @Override
    protected void check(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
    }
    @Override
    protected void check(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final VmInstanceSpec.ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg);
            return;
        }

        final VolumeInventory volume = msg.getVolume();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("external-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache;
            String installPath;
            String format;
            Long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        downloadImageCache(msg.getTemplateSpec().getInventory(), new ReturnValueCompletion<ImageCacheInventory>(trigger) {
                            @Override
                            public void success(ImageCacheInventory returnValue) {
                                pathInCache = returnValue.getInstallUrl();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildVolumeName(volume.getUuid()));
                        spec.setUuid(volume.getUuid());
                        spec.setSize(volume.getSize());
                        controller.cloneVolume(pathInCache, spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats returnValue) {
                                actualSize = returnValue.getActualSize();
                                installPath = returnValue.getInstallPath();
                                format = returnValue.getFormat();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        volume.setInstallPath(installPath);
                        volume.setActualSize(actualSize);
                        volume.setFormat(format);
                        volume.setProtocol(externalVO.getDefaultProtocol());
                        reply.setVolume(volume);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void createEmptyVolume(InstantiateVolumeOnPrimaryStorageMsg msg) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        VolumeInventory volume = msg.getVolume();
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setName(buildVolumeName(volume.getUuid()));
        spec.setSize(volume.getSize());
        spec.setAllocatedUrl(msg.getAllocatedInstallUrl());

        controller.createVolume(spec, new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                volume.setActualSize(stats.getActualSize());
                volume.setSize(stats.getSize());
                volume.setFormat(stats.getFormat());
                volume.setInstallPath(stats.getInstallPath());
                volume.setProtocol(externalVO.getDefaultProtocol());
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        controller.deleteVolume(msg.getVolume().getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();


        BackupStorageSelector selector = pluginRgty.getExtensionFromMap(externalVO.getIdentity(), BackupStorageSelector.class);
        List<String> preferBsTypes = selector.getPreferBackupStorageTypes();
        if (!CollectionUtils.isEmpty(msg.getRequiredBackupStorageTypes())) {
            preferBsTypes.removeAll(msg.getRequiredBackupStorageTypes());
        }

        if (CollectionUtils.isEmpty(preferBsTypes)) {
            reply.setError(operr("no backup storage type specified support to primary storage[uuid:%s]", self.getUuid()));
        }

        List<BackupStorageVO> availableBs = SQL.New("select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref" +
                " where bs.uuid = ref.backupStorageUuid" +
                " and ref.zoneUuid = :zoneUuid" +
                " and bs.status = :status" +
                " and bs.state = :state" +
                " and bs.availableCapacity > :size", BackupStorageVO.class)
                .param("zoneUuid", self.getZoneUuid())
                .param("status", BackupStorageStatus.Connected)
                .param("state", BackupStorageState.Enabled)
                .param("size", msg.getRequiredSize())
                .list();

        // sort by prefer type
        availableBs.sort(Comparator.comparingInt(o -> preferBsTypes.indexOf(o.getType())));
        reply.setInventory(BackupStorageInventory.valueOf(availableBs.get(0)));

        bus.reply(msg, reply);
    }

    private void handle(TakeSnapshotMsg msg) {
        TakeSnapshotReply reply = new TakeSnapshotReply();
        VolumeSnapshotInventory sp = msg.getStruct().getCurrent();

        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class));
        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
        sspec.setVolumeInstallPath(vol.getInstallPath());
        sspec.setName(buildSnapshotName(sp.getUuid()));
        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(msg) {
            @Override
            public void success(VolumeSnapshotStats stats) {
                sp.setPrimaryStorageInstallPath(stats.getInstallPath());
                sp.setSize(stats.getActualSize());
                reply.setInventory(sp);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    // TODO
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-%s-on-primary-storage-%s", msg.getVolumeInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String imageCachePath;

            String snapshotPath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotSpec spec = new CreateVolumeSnapshotSpec();
                        spec.setUuid(Platform.getUuid());
                        spec.setVolumeInstallPath(msg.getVolumeInventory().getInstallPath());
                        controller.createSnapshot(spec, new ReturnValueCompletion<VolumeSnapshotStats>(trigger) {
                            @Override
                            public void success(VolumeSnapshotStats stats) {
                                snapshotPath = stats.getInstallPath();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (snapshotPath == null) {
                            trigger.rollback();
                            return;
                        }

                        controller.deleteSnapshot(snapshotPath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "clone-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildImageName(msg.getImageInventory().getUuid()));
                        spec.setUuid(msg.getVolumeInventory().getUuid());
                        spec.setSize(msg.getVolumeInventory().getSize());
                        controller.cloneVolume(snapshotPath, spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats dst) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageCachePath == null) {
                            trigger.rollback();
                            return;
                        }
                        controller.deleteVolume(imageCachePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "flatten-volume";

                    @Override
                    public boolean skip(Map data) {
                        return msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.flattenVolume(imageCachePath, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats stats) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-snapshot-%s-on-primary-storage-%s", msg.getVolumeSnapshot().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String imageCachePath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "clone-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildImageName(msg.getImageInventory().getUuid()));
                        spec.setUuid(msg.getImageInventory().getUuid());
                        spec.setSize(msg.getImageInventory().getSize());
                        controller.cloneVolume(msg.getVolumeSnapshot().getPrimaryStorageInstallPath(), spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats dst) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageCachePath == null) {
                            trigger.rollback();
                            return;
                        }
                        controller.deleteVolume(imageCachePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "flatten-volume";

                    @Override
                    public boolean skip(Map data) {
                        return msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.flattenVolume(imageCachePath, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats stats) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-%s-on-primary-storage-%s", msg.getVolumeInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            RemoteTarget remoteTarget;

            String snapshotPath;
            String bsInstallPath;

            long templateSize;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        // TODO volume is online
                        return false;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();

                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(msg.getVolumeInventory().getUuid());
                        cmsg.setName("snapshot-for-template-" + msg.getImageInventory().getUuid());
                        cmsg.setVolumeUuid(msg.getVolumeInventory().getUuid());
                        cmsg.setAccountUuid(volumeAccountUuid);
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    snapshotPath = ((CreateVolumeSnapshotReply) reply).getInventory().getPrimaryStorageInstallPath();
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (snapshotPath == null) {
                            trigger.rollback();
                            return;
                        }

                        controller.deleteSnapshot(snapshotPath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "export-volume-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExportSpec espec = new ExportSpec();
                        espec.setInstallPath(snapshotPath);
                        espec.setClientIp(getClientIp(msg.getBackupStorageUuid()));
                        espec.setClientName(msg.getBackupStorageUuid());

                        controller.export(espec, NvmeRemoteTarget.class, new ReturnValueCompletion<NvmeRemoteTarget>(trigger) {
                            @Override
                            public void success(NvmeRemoteTarget returnValue) {
                                remoteTarget = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (remoteTarget == null) {
                            trigger.rollback();
                            return;
                        }
                        controller.unexport(snapshotPath, remoteTarget, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "import-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DownloadImageFromRemoteTargetMsg umsg = new DownloadImageFromRemoteTargetMsg();
                        umsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        umsg.setImageUuid(msg.getImageInventory().getUuid());
                        umsg.setRemoteTargetUrl(remoteTarget.getResourceURI());
                        bus.makeTargetServiceIdByResourceUuid(umsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(umsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    DownloadImageFromRemoteTargetReply r = reply.castReply();
                                    bsInstallPath = r.getInstallPath();
                                    templateSize = r.getSize();
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unexport-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.unexport(snapshotPath, remoteTarget, new Completion(trigger) {
                            @Override
                            public void success() {
                                // prevent to rollback again.
                                remoteTarget = null;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setTemplateBackupStorageInstallPath(bsInstallPath);
                        reply.setFormat(msg.getVolumeInventory().getFormat());
                        reply.setActualSize(templateSize);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private String getClientIp(String bsUuid) {
        GetBackupStorageManagerHostnameMsg msg = new GetBackupStorageManagerHostnameMsg();
        msg.setUuid(bsUuid);
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }
        return ((GetBackupStorageManagerHostnameReply) reply).getHostname();
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setAllocatedUrl(msg.getAllocatedInstallUrl());
        spec.setName(buildVolumeName(msg.getVolumeUuid()));
        spec.setUuid(msg.getVolumeUuid());
        downloadImageTo(msg.getImage(), spec, VolumeVO.class.getSimpleName(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats returnValue) {
                reply.setInstallPath(returnValue.getInstallPath());
                reply.setFormat(msg.getImage().getFormat());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void downloadImageCache(ImageInventory image, ReturnValueCompletion<ImageCacheInventory> completion) {
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setUuid(image.getUuid());
        spec.setName(buildImageName(image.getUuid()));

        ImageCacheVO cache = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, image.getUuid())
                .find();
        if (cache != null) {
            // TODO check exists in ps
            completion.success(ImageCacheInventory.valueOf(cache));
            return;
        }

        downloadImageTo(image, spec, ImageCacheVO.class.getSimpleName(), new ReturnValueCompletion<VolumeStats>(completion) {
            @Override
            public void success(VolumeStats volStats) {
                ImageCacheVO cache = new ImageCacheVO();
                if (image.getMd5Sum() == null) {
                    cache.setMd5sum("not calculated");
                } else {
                    cache.setMd5sum(image.getMd5Sum());
                }
                cache.setImageUuid(image.getUuid());
                cache.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
                cache.setInstallUrl(volStats.getInstallPath());
                cache.setSize(volStats.getSize());
                cache.setPrimaryStorageUuid(self.getUuid());
                cache.setInstallUrl(volStats.getInstallPath());
                dbf.persist(cache);
                completion.success(ImageCacheInventory.valueOf(cache));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void downloadImageTo(ImageInventory image, CreateVolumeSpec spec, String targetClz, ReturnValueCompletion<VolumeStats> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-to-%s", image.getUuid(), spec.getName()));
        chain.then(new ShareFlow() {
            RemoteTarget remoteTarget;
            VolumeStats volume;

            String bsUuid = image.getBackupStorageRefs().get(0).getBackupStorageUuid();
            String bsInstallPath = image.getBackupStorageRefs().get(0).getInstallPath();

            // TODO: hardcode
            VolumeProtocol protocol = VolumeProtocol.NVMEoF;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        spec.setSize(image.getSize());
                        controller.createVolume(spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats stats) {
                                volume = stats;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (volume == null) {
                            trigger.rollback();
                            return;
                        }

                        controller.deleteVolume(volume.getInstallPath(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "export-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExportSpec espec = new ExportSpec();
                        espec.setInstallPath(volume.getInstallPath());
                        espec.setClientIp(getClientIp(bsUuid));
                        espec.setClientName(bsUuid);
                        controller.export(espec, NvmeRemoteTarget.class, new ReturnValueCompletion<NvmeRemoteTarget>(trigger) {
                            @Override
                            public void success(NvmeRemoteTarget returnValue) {
                                remoteTarget = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (remoteTarget == null) {
                            trigger.rollback();
                            return;
                        }
                        controller.unexport(volume.getInstallPath(), remoteTarget, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExportImageToRemoteTargetMsg dmsg = new ExportImageToRemoteTargetMsg();
                        dmsg.setBackupStorageUuid(bsUuid);
                        dmsg.setInstallPath(bsInstallPath);
                        dmsg.setRemoteTargetUrl(remoteTarget.getResourceURI());
                        // TODO hardcode
                        dmsg.setFormat(controller.reportCapabilities().getSupportedImageFormats().get(0));
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unexport-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.unexport(volume.getInstallPath(), remoteTarget, new Completion(trigger) {
                            @Override
                            public void success() {
                                // prevent to rollback again.
                                remoteTarget = null;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-snapshot-if-needed";

                    @Override
                    public boolean skip(Map data) {
                        return !targetClz.equals(ImageCacheVO.class.getSimpleName()) || controller.reportCapabilities().isSupportCloneFromVolume();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
                        sspec.setVolumeInstallPath(volume.getInstallPath());
                        sspec.setName(spec.getName());
                        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(trigger) {
                            @Override
                            public void success(VolumeSnapshotStats returnValue) {
                                volume.setInstallPath(returnValue.getInstallPath());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(volume);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setSize(msg.getImage().getSize());
        aspec.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume);

        String installPath = controller.allocateSpace(aspec);
        reply.setInstallPath(installPath);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        deleteBits(msg.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        deleteBits(msg.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {
        // TODO
        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();

        downloadImageCache(msg.getIsoSpec().getInventory(), new ReturnValueCompletion<ImageCacheInventory>(msg) {
            @Override
            public void success(ImageCacheInventory returnValue) {
                reply.setInstallPath(returnValue.getInstallUrl());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        // The ISO is in the image cache, no need to delete it
        bus.reply(msg, new DeleteIsoFromPrimaryStorageReply());
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        reply.setCapability(controller.reportCapabilities().getSnapshotCapability());
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        controller.stats(msg.getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
        controller.stats(msg.getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();

        Map<String, String> installPathToUuids = msg.getVolumeUuidInstallPaths().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getValue, Map.Entry::getKey, (k1, k2) -> k1
        ));
        controller.batchStats(msg.getVolumeUuidInstallPaths().values(), new ReturnValueCompletion<List<VolumeStats>>(msg) {
            @Override
            public void success(List<VolumeStats> stats) {
                Map<String, Long> actualSizeByUuids = stats.stream().collect(Collectors.toMap(
                        s -> installPathToUuids.get(s.getInstallPath()), VolumeStats::getActualSize, (k1, k2) -> k1
                ));
                reply.setActualSizes(actualSizeByUuids);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        controller.deleteSnapshot(msg.getFrom().getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        controller.flattenVolume(msg.getVolume().getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        controller.deleteSnapshot(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        controller.revertVolumeSnapshot(msg.getSnapshot().getPrimaryStorageInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setNewVolumeInstallPath(stats.getInstallPath());
                reply.setSize(stats.getSize());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handle(AskInstallPathForNewSnapshotMsg msg) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        reply.setSnapshotInstallPath(controller.buildVolumeSnapshotInstallPath(msg.getVolumeInventory().getInstallPath(), msg.getSnapshotUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        bus.reply(msg, new CheckVolumeSnapshotOperationOnPrimaryStorageReply());
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        controller.connect(externalVO.getConfig(), self.getUrl(), new ReturnValueCompletion<LinkedHashMap>(completion) {
            @Override
            public void success(LinkedHashMap addonInfo) {
                SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                        .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                        .update();

                // to update capacity
                pingHook(completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void pingHook(Completion completion) {
        controller.reportCapacity(new ReturnValueCompletion<StorageCapacity>(completion) {
            @Override
            public void success(StorageCapacity capacity) {
                if (capacity.getHealthy() == StorageHealthy.Ok) {
                    new PrimaryStorageCapacityUpdater(self.getUuid()).run(new PrimaryStorageCapacityUpdaterRunnable() {
                        @Override
                        public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                                cap.setAvailableCapacity(capacity.getAvailableCapacity());
                            }

                            cap.setTotalCapacity(capacity.getTotalCapacity());
                            cap.setTotalPhysicalCapacity(capacity.getTotalCapacity());
                            cap.setAvailablePhysicalCapacity(capacity.getAvailableCapacity());

                            return cap;
                        }
                    });
                    completion.success();
                } else {
                    completion.fail(operr("storage is not healthy:%s", capacity.getHealthy().toString()));
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        controller.reportCapacity(new ReturnValueCompletion<StorageCapacity>(completion) {
            @Override
            public void success(StorageCapacity usage) {
                PhysicalCapacityUsage ret = new PhysicalCapacityUsage();
                ret.totalPhysicalSize = usage.getTotalCapacity();
                ret.availablePhysicalSize = usage.getAvailableCapacity();
                completion.success(ret);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        // TODO
        throw new UnsupportedOperationException();
    }

    private void deleteBits(String installPath, Completion completion) {
        controller.deleteVolume(installPath, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
