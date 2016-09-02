package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotReply.CreateTemplateFromVolumeSnapshotResult;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.storage.backup.BackupStoragePathMaker;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO;
import org.zstack.storage.backup.sftp.SftpBackupStorageVO_;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.header.storage.primary.PrimaryStorageManager;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.*;
import org.zstack.storage.primary.iscsi.IscsiIsoStoreManager.IscsiIsoSpec;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiFilesystemBackendPrimaryStorage extends PrimaryStorageBase {
    private CLogger logger = Utils.getLogger(IscsiFilesystemBackendPrimaryStorage.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private IscsiFileSystemBackendPrimaryStorageFactory factory;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PrimaryStorageManager psMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private IscsiIsoStoreManager isoStoreMgr;

    public IscsiFilesystemBackendPrimaryStorage(IscsiFileSystemBackendPrimaryStorageVO vo) {
        super(vo);
    }

    protected IscsiFileSystemBackendPrimaryStorageVO getSelf() {
        return (IscsiFileSystemBackendPrimaryStorageVO) self;
    }

    @Override
    protected IscsiFileSystemBackendPrimaryStorageInventory getSelfInventory() {
        return IscsiFileSystemBackendPrimaryStorageInventory.valueOf(getSelf());
    }

    @Override
    protected void handle(InstantiateVolumeMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateMsg) {
            handle((InstantiateRootVolumeFromTemplateMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    private String makeHttpUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        ub.host(getSelf().getHostname());
        ub.port(IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PORT);
        ub.path(getSelf().getFilesystemType());
        ub.path(path);
        return ub.build().toUriString();
    }

    private String makePathInImageCache(String imageUuid) {
        return PathUtil.join(self.getUrl(), "imageCache/templates", imageUuid, String.format("%s.template", imageUuid));
    }

    private String makeIsoPathInImageCache(String imageUuid) {
        return PathUtil.join(self.getUrl(), "imageCache/iso", imageUuid, String.format("%s.iso", imageUuid));
    }

    private String makeIsoSubvolumePathInImageCache(String imageUuid) {
        return PathUtil.join(self.getUrl(), "imageCache/iso/subvolumes",imageUuid, imageUuid);
    }

    private String makeRootVolumePath(String volumeUuid) {
        return PathUtil.join(self.getUrl(), "rootVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s", volumeUuid), String.format("%s.img", volumeUuid));
    }

    private String makeRootVolumeBySnapshotPath(String volumeUuid, String snapshotUuid) {
        return PathUtil.join(self.getUrl(), "rootVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s-by-snapshot", volumeUuid), String.format("snapshot-%s", snapshotUuid), String.format("random-%s", Platform.getUuid()));
    }

    private String makeDataVolumePath(String volumeUuid) {
        return PathUtil.join(self.getUrl(), "dataVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s", volumeUuid), String.format("%s.img", volumeUuid));
    }

    private String makeVolumeSnapshotPath(String volumeUuid, String snapshotUuid) {
        return PathUtil.join(self.getUrl(), "snapshots", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(snapshotUuid)),
                String.format("vol-%s", volumeUuid), snapshotUuid);
    }

    private String makeSnapshotWorkSpaceInstallPath(String snapshotUuid) {
        return PathUtil.join(self.getUrl(), "workspace", "snapshot", snapshotUuid, String.format("%s.temp-snapshot", snapshotUuid));
    }

    //TODO: merge with makeDataVolumePath
    private String makeDataVolumeSubvolumePath(String volumeUuid) {
        return PathUtil.join(self.getUrl(), "dataVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s", volumeUuid));
    }

    private String findSftpRootPath(String bsUuid) {
        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.select(SftpBackupStorageVO_.url);
        q.add(SftpBackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
        return q.findValue();
    }

    private String makeRootVolumeTemplateInstallPathOnBackupStorage(String bsUuid, String imageUuid) {
        return PathUtil.join(
                findSftpRootPath(bsUuid),
                BackupStoragePathMaker.makeRootVolumeTemplateInstallFolderPath(imageUuid),
                String.format("%s.template", imageUuid)
        );
    }

    private String makeDataVolumeTemplateInstallPath(String backupStorageUuid, String volumeUuid) {
        return PathUtil.join(
                findSftpRootPath(backupStorageUuid),
                BackupStoragePathMaker.makeDataVolumeTemplateInstallFolderPath(volumeUuid),
                String.format("%s.template", volumeUuid)
        );
    }

    public String makeVolumeSnapshotInstallPathOnBackupStorage(String bsUuid, String snapshotUuid) {
        return PathUtil.join(
                findSftpRootPath(bsUuid),
                BackupStoragePathMaker.makeVolumeSnapshotInstallFolderPath(snapshotUuid),
                String.format("%s.raw", snapshotUuid)
        );
    }

    private String makeIscsiVolumePath(String target, String volumePath) {
        IscsiVolumePath path = new IscsiVolumePath();
        path.setInstallPath(volumePath);
        path.setHostname(getSelf().getHostname());
        path.setTarget(target);
        return path.assemble();
    }

    private String makeIscsiTargetName(String uuid) {
        return String.format("iqn.%s.org.zstack:%s", new SimpleDateFormat("yyyy-MM").format(new Date()), uuid);
    }

    private void deleteBits(String installPath, String volumeUuid, final Completion completion) {
        DeleteBitsCmd cmd = new DeleteBitsCmd();
        if (installPath.startsWith("iscsi")) {
            IscsiVolumePath path = new IscsiVolumePath(installPath);
            path.disassemble();
            String volumeInstallPath = path.getInstallPath();
            cmd.setInstallPath(volumeInstallPath);
            cmd.setIscsiPath(path.assembleIscsiPath());
        } else {
            cmd.setInstallPath(installPath);
        }
        cmd.setVolumeUuid(volumeUuid);
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(DeleteBitsRsp ret) {
                if (ret.isSuccess()) {
                    reportCapacity(ret);
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(ret.getError()));
                }
            }

            @Override
            public Class<DeleteBitsRsp> getReturnClass() {
                return DeleteBitsRsp.class;
            }
        });
    }

    private class DownloadImageToCache {
        ImageCacheVO result;
        BackupStorageInventory bs;
        ImageSpec imageSpec;

        void run(final ReturnValueCompletion<ImageCacheVO> completion) {
            DebugUtils.Assert(imageSpec != null, "imageSpec cannot be null");
            DebugUtils.Assert(bs != null, "backup storage cannot bel null");

            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return getName();
                }

                @Override
                public void run(final SyncTaskChain chain) {
                    Completion c = new Completion(chain, completion) {
                        @Override
                        public void success() {
                            completion.success(result);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    };

                    SimpleQuery<ImageCacheVO> query = dbf.createQuery(ImageCacheVO.class);
                    query.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                    query.add(ImageCacheVO_.imageUuid, Op.EQ, imageSpec.getInventory().getUuid());
                    ImageCacheVO cvo = query.find();
                    if (cvo != null) {
                        checkCache(cvo, c);
                    } else {
                        downloadToCache(null, c);
                    }
                }

                @Override
                public String getName() {
                    return String.format("download-image-%s-to-iscsi-primary-storage-%s", imageSpec.getInventory().getUuid(), self.getUuid());
                }
            });
        }

        private void downloadToCache(final ImageCacheVO cvo, final Completion completion) {
            final IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()));

            final String imagePathInCache = ImageMediaType.ISO.toString().equals(imageSpec.getInventory().getMediaType()) ?
                    makeIsoPathInImageCache(imageSpec.getInventory().getUuid()) : makePathInImageCache(imageSpec.getInventory().getUuid());

            FlowChain chain  = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("download-image-%s-to-iscsi-primary-storage-%s", imageSpec.getInventory().getUuid(), self.getUuid()));
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    if (cvo == null) {
                        flow(new Flow() {
                            String __name__ = "allocate-primary-storage";

                            boolean s = true;

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                                amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                                amsg.setSize(imageSpec.getInventory().getSize());
                                amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                                amsg.setNoOverProvisioning(true);
                                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                                bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                        } else {
                                            s = true;
                                            trigger.next();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void rollback(FlowRollback trigger, Map data) {
                                if (s) {
                                    ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
                                    rmsg.setPrimaryStorageUuid(self.getUuid());
                                    rmsg.setDiskSize(imageSpec.getInventory().getSize());
                                    rmsg.setNoOverProvisioning(true);
                                    bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                    bus.send(rmsg);
                                }

                                trigger.rollback();
                            }
                        });
                    }

                    flow(new NoRollbackFlow() {
                        String __name__ = "download";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            mediator.downloadBits(getSelfInventory(), bs,imageSpec.getSelectedBackupStorage().getInstallPath(), imagePathInCache, new Completion(trigger) {
                                @Override
                                public void success() {
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
                            if (cvo == null) {
                                ImageCacheVO vo = new ImageCacheVO();
                                vo.setImageUuid(imageSpec.getInventory().getUuid());
                                vo.setInstallUrl(imagePathInCache);
                                vo.setMediaType(ImageMediaType.valueOf(imageSpec.getInventory().getMediaType()));
                                vo.setPrimaryStorageUuid(self.getUuid());
                                vo.setSize(imageSpec.getInventory().getSize());
                                vo.setState(ImageCacheState.ready);
                                vo.setMd5sum("not calculated");
                                dbf.persist(vo);
                                result = vo;
                            } else {
                                cvo.setInstallUrl(imagePathInCache);
                                dbf.update(cvo);
                                result = cvo;
                            }

                            completion.success();
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

        private void checkCache(final ImageCacheVO cvo, final Completion completion) {
            CheckBitsExistenceCmd cmd = new CheckBitsExistenceCmd();
            cmd.setPath(cvo.getInstallUrl());
            restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CHECK_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<CheckBitsExistenceRsp>(completion) {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(err);
                }

                @Override
                public void success(CheckBitsExistenceRsp ret) {
                    if (!ret.isSuccess()) {
                        completion.fail(errf.stringToOperationError(ret.getError()));
                    }  else {
                        if (ret.isExisting()) {
                            result = cvo;
                            completion.success();
                        } else {
                            downloadToCache(cvo, completion);
                        }
                    }
                }

                @Override
                public Class<CheckBitsExistenceRsp> getReturnClass() {
                    return CheckBitsExistenceRsp.class;
                }
            });
        }
    }

    private void handle(final InstantiateRootVolumeFromTemplateMsg msg) {
        final InstantiateVolumeReply reply = new InstantiateVolumeReply();
        final ImageSpec ispec = msg.getTemplateSpec();

        BackupStorageVO bs = dbf.findByUuid(ispec.getSelectedBackupStorage().getBackupStorageUuid(), BackupStorageVO.class);
        final BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bs);

        final VolumeInventory volume = msg.getVolume();
        final ImageInventory image = ispec.getInventory();
        if (ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("create-root-volume-from-image-%s", image.getUuid()));
            chain.then(new ShareFlow() {
                String imagePathInCache;
                String volumePath = makeRootVolumePath(volume.getUuid());
                String iscsiTarget;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("download-image-%s-to-cache-ps-%s", image.getUuid(), self.getUuid());

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DownloadImageToCache download = new DownloadImageToCache();
                            download.imageSpec = ispec;
                            download.bs = bsinv;
                            download.run(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                                @Override
                                public void success(ImageCacheVO returnValue) {
                                    imagePathInCache = returnValue.getInstallUrl();
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = String.format("create-volume-%s-from-image-%s", volume.getUuid(), image.getUuid());

                        public void run(final FlowTrigger trigger, Map data) {
                            CreateRootVolumeFromTemplateCmd cmd = new CreateRootVolumeFromTemplateCmd();
                            cmd.setInstallPath(volumePath);
                            cmd.setVolumeUuid(volume.getUuid());
                            cmd.setChapPassword(getSelf().getChapPassword());
                            cmd.setChapUsername(getSelf().getChapUsername());
                            cmd.setTemplatePathInCache(imagePathInCache);

                            restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_ROOT_VOLUME_PATH),
                                    cmd, new JsonAsyncRESTCallback<CreateRootVolumeFromTemplateRsp>() {
                                        @Override
                                        public void fail(ErrorCode err) {
                                            trigger.fail(err);
                                        }

                                        @Override
                                        public void success(CreateRootVolumeFromTemplateRsp ret) {
                                            if (!ret.isSuccess()) {
                                                volumePath = null;
                                                trigger.fail(errf.stringToOperationError(ret.getError()));
                                            } else {
                                                iscsiTarget = makeIscsiVolumePath(ret.getIscsiPath(), volumePath);
                                                reportCapacity(ret);
                                                trigger.next();
                                            }
                                        }

                                        @Override
                                        public Class<CreateRootVolumeFromTemplateRsp> getReturnClass() {
                                            return CreateRootVolumeFromTemplateRsp.class;
                                        }
                                    });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (volumePath != null) {
                                deleteBits(volumePath, volume.getUuid(), new NopeCompletion());
                            }

                            trigger.rollback();
                        }
                    });

                    done(new FlowDoneHandler(msg) {
                        @Override
                        public void handle(Map data) {
                            volume.setInstallPath(iscsiTarget);
                            reply.setVolume(volume);
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
        } else {
            createEmptyVolume(msg);
        }
    }

    private void reportCapacity(AgentCapacityResponse ret) {
        if (ret.getTotalCapacity() != null && ret.getAvailableCapacity() != null) {
            new PrimaryStorageCapacityUpdater(self.getUuid()).updateAvailablePhysicalCapacity(ret.getAvailableCapacity());
        }
    }

    private void createEmptyVolume(final InstantiateVolumeMsg msg) {
        final VolumeInventory vol = msg.getVolume();
        CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        final InstantiateVolumeReply reply = new InstantiateVolumeReply();

        final String volPath;
        if (VolumeType.Root.toString().equals(vol.getType())) {
            volPath = makeRootVolumePath(vol.getUuid());
        } else {
            volPath = makeDataVolumePath(vol.getUuid());
        }

        cmd.setInstallPath(volPath);
        cmd.setVolumeUuid(vol.getUuid());
        cmd.setChapUsername(getSelf().getChapUsername());
        cmd.setChapPassword(getSelf().getChapPassword());
        cmd.setSize(vol.getSize());
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_EMPTY_VOLUME_PATH), cmd, new JsonAsyncRESTCallback<CreateEmptyVolumeRsp>() {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateEmptyVolumeRsp ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                } else {
                    vol.setInstallPath(makeIscsiVolumePath(ret.getIscsiPath(), volPath));
                    reply.setVolume(vol);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<CreateEmptyVolumeRsp> getReturnClass() {
                return CreateEmptyVolumeRsp.class;
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        deleteBits(msg.getVolume().getInstallPath(), msg.getVolume().getUuid(), new Completion(msg) {
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
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);
        BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);
        VolumeInventory vol = msg.getVolumeInventory();
        String imageUuid = msg.getImageInventory().getUuid();

        IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsinv.getType()));

        String bsInstallPath = null;
        if (vol.getType().equals(VolumeType.Root.toString())) {
            bsInstallPath = mediator.makeRootVolumeTemplateInstallPath(bsinv, imageUuid);
        } else if (vol.getType().equals(VolumeType.Data.toString())) {
            bsInstallPath = mediator.makeDataVolumeTemplateInstallPath(bsinv, imageUuid);
        } else {
            DebugUtils.Assert(false, String.format("unknown volume type[%s]", vol.getType()));
        }

        IscsiVolumePath path = new IscsiVolumePath(vol.getInstallPath()).disassemble();
        final String finalBsInstallPath = bsInstallPath;
        mediator.uploadBits(getSelfInventory(), bsinv, bsInstallPath, path.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                reply.setTemplateBackupStorageInstallPath(finalBsInstallPath);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
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
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        final String installPath = makeDataVolumePath(msg.getVolumeUuid());
        BackupStorageVO bsvo = dbf.findByUuid(msg.getBackupStorageRef().getBackupStorageUuid(), BackupStorageVO.class);
        final BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

        final IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsinv.getType()));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-to-volume-%s-to-iscsi-primary-storage", msg.getImage().getUuid(), msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            String target;
            Integer lun;

            @Override
            public void setup() {
                flow(new Flow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        mediator.downloadBits(getSelfInventory(), bsinv, msg.getBackupStorageRef().getInstallPath(),
                                installPath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        deleteBits(installPath, vol.getUuid(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: GC
                                logger.warn(String.format("failed to delete %s on primary storage[uuid:%s], %s. Need GC for this rollback error",
                                        installPath, self.getUuid(), errorCode));
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateIscsiTargetCmd cmd = new CreateIscsiTargetCmd();
                        cmd.setInstallPath(installPath);
                        cmd.setVolumeUuid(vol.getUuid());
                        cmd.setChapPassword(getSelf().getChapPassword());
                        cmd.setChapUsername(getSelf().getChapUsername());
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_TARGET_PATH), cmd, new JsonAsyncRESTCallback<CreateIscsiTargetRsp>() {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CreateIscsiTargetRsp ret) {
                                if (!ret.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(ret.getError()));
                                } else {
                                    target = ret.getTarget();
                                    lun = ret.getLun();
                                    trigger.next();
                                }
                            }

                            @Override
                            public Class<CreateIscsiTargetRsp> getReturnClass() {
                                return CreateIscsiTargetRsp.class;
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        IscsiVolumePath path = new IscsiVolumePath();
                        path.setInstallPath(installPath);
                        path.setHostname(getSelfInventory().getHostname());
                        path.setLun(lun);
                        path.setTarget(target);
                        reply.setInstallPath(path.assemble());
                        reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
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

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        deleteBits(msg.getInstallPath(), null, new Completion(msg) {
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
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();

        // check if the store has a spare one, if there is then return it
        IscsiIsoSpec spec = new IscsiIsoSpec();
        spec.setVmInstanceUuid(msg.getVmInstanceUuid());
        spec.setImageUuid(msg.getIsoSpec().getInventory().getUuid());
        spec.setPrimaryStorageUuid(self.getUuid());
        IscsiIsoVO isovo = isoStoreMgr.take(spec);
        if (isovo != null) {
            reply.setInstallPath(isovo.getPath());
            bus.reply(msg, reply);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-iso-%s-to-iscsi-btrfs-primary-storage-%s", msg.getIsoSpec().getInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache;
            String isoSubVolumePath;
            String iscsiPath;
            String iscsiTarget;
            String isoSubvolumeUuid;
            int iscsiLun;
            String isoUuid = Platform.getUuid();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-to-image-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadImageToCache download = new DownloadImageToCache();
                        download.imageSpec = msg.getIsoSpec();
                        BackupStorageVO bsvo = dbf.findByUuid(msg.getIsoSpec().getSelectedBackupStorage().getBackupStorageUuid(), BackupStorageVO.class);
                        download.bs = BackupStorageInventory.valueOf(bsvo);
                        download.run(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                            @Override
                            public void success(ImageCacheVO returnValue) {
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

                flow(new Flow() {
                    String __name__ = "create-subvolume-for-iso";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final String dstFolder = makeIsoSubvolumePathInImageCache(isoUuid);
                        CreateSubVolumeCmd cmd = new CreateSubVolumeCmd();
                        cmd.setSrc(pathInCache);
                        cmd.setDst(dstFolder);
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_SUBVOLUME), cmd, new JsonAsyncRESTCallback<CreateSubVolumeRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CreateSubVolumeRsp ret) {
                                if (ret.isSuccess()) {
                                    reportCapacity(ret);
                                    isoSubVolumePath = ret.getPath();
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.stringToOperationError(ret.getError()));
                                }
                            }

                            @Override
                            public Class<CreateSubVolumeRsp> getReturnClass() {
                                return CreateSubVolumeRsp.class;
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (isoSubVolumePath == null) {
                            trigger.rollback();
                            return;
                        }

                        deleteBits(isoSubVolumePath, null, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to deleted subvolume[%s], %s, continue to rollback", isoSubVolumePath, errorCode));
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-iscsi-target-for-iso";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateIscsiTargetCmd cmd = new CreateIscsiTargetCmd();
                        cmd.setChapPassword(getSelf().getChapPassword());
                        cmd.setChapUsername(getSelf().getChapUsername());
                        cmd.setInstallPath(isoSubVolumePath);
                        isoSubvolumeUuid = String.format("%s-%s", msg.getIsoSpec().getInventory().getUuid(), isoUuid);
                        cmd.setVolumeUuid(isoSubvolumeUuid);
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_TARGET_PATH), cmd, new JsonAsyncRESTCallback<CreateIscsiTargetRsp>() {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CreateIscsiTargetRsp ret) {
                                if (ret.isSuccess()) {
                                    IscsiVolumePath path = new IscsiVolumePath();
                                    path.setInstallPath(isoSubVolumePath);
                                    path.setHostname(getSelfInventory().getHostname());
                                    path.setLun(ret.getLun());
                                    path.setTarget(ret.getTarget());
                                    iscsiPath = path.assemble();
                                    iscsiTarget = ret.getTarget();
                                    iscsiLun = ret.getLun();
                                    trigger.next();
                                } else {
                                    trigger.fail(errf.stringToOperationError(ret.getError()));
                                }
                            }

                            @Override
                            public Class<CreateIscsiTargetRsp> getReturnClass() {
                                return CreateIscsiTargetRsp.class;
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (iscsiTarget == null) {
                            trigger.rollback();
                            return;
                        }

                        DeleteIscsiTargetCmd cmd = new DeleteIscsiTargetCmd();
                        cmd.setTarget(iscsiTarget);
                        cmd.setUuid(isoSubvolumeUuid);
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_TARGET_PATH), cmd, new JsonAsyncRESTCallback<DeleteIscsiTargetRsp>() {
                            @Override
                            public void fail(ErrorCode err) {
                                logger.warn(String.format("failed delete iscsi target[target:%s, sub volume uuid:%s], %s",
                                        iscsiTarget, isoSubvolumeUuid, err));
                                trigger.rollback();
                            }

                            @Override
                            public void success(DeleteIscsiTargetRsp ret) {
                                if (!ret.isSuccess()) {
                                    logger.warn(String.format("failed to delete iscsi target[name:%s, uuid:%s], %s", iscsiTarget, isoSubvolumeUuid, ret.getError()));
                                }
                                trigger.rollback();
                            }

                            @Override
                            public Class<DeleteIscsiTargetRsp> getReturnClass() {
                                return DeleteIscsiTargetRsp.class;
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "save-iso-to-iscsi-iso-store";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IscsiIsoVO vo = new IscsiIsoVO();
                        vo.setUuid(isoUuid);
                        vo.setVmInstanceUuid(msg.getVmInstanceUuid());
                        vo.setImageUuid(msg.getIsoSpec().getInventory().getUuid());
                        vo.setPrimaryStorageUuid(self.getUuid());
                        vo.setHostname(getSelf().getHostname());
                        vo.setPort(3260);
                        vo.setLun(iscsiLun);
                        vo.setPath(iscsiPath);
                        vo.setTarget(iscsiTarget);
                        isoStoreMgr.store(vo);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setInstallPath(iscsiPath);
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

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        isoStoreMgr.releaseByVmUuid(msg.getVmInstanceUuid());
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
        capability.setSupport(true);
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeActualSizeOnPrimaryStorageMsg msg) {
        throw new CloudRuntimeException("not supported");
    }

    @Override
    protected void connectHook(ConnectPrimaryStorageMsg msg, Completion completion) {
        connect(completion);
    }

    @Override
    protected void syncPhysicalCapacity(final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        GetCapacityCmd cmd = new GetCapacityCmd();
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.GET_CAPACITY), cmd, new JsonAsyncRESTCallback<AgentCapacityResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(AgentCapacityResponse ret) {
                if (!ret.isSuccess()) {
                    completion.fail(errf.stringToOperationError(ret.getError()));
                } else {
                    PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
                    usage.availablePhysicalSize = ret.getAvailableCapacity();
                    usage.totalPhysicalSize = ret.getTotalCapacity();
                    completion.success(usage);
                }
            }

            @Override
            public Class<AgentCapacityResponse> getReturnClass() {
                return AgentCapacityResponse.class;
            }
        });
    }

    @Override
    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        final APIReconnectPrimaryStorageEvent evt  = new APIReconnectPrimaryStorageEvent(msg.getId());
        self.setStatus(PrimaryStorageStatus.Connecting);
        self = dbf.updateAndRefresh(self);
        connect(new Completion(msg) {
            @Override
            public void success() {
                self.setStatus(PrimaryStorageStatus.Connected);
                self = dbf.updateAndRefresh(self);
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                self.setStatus(PrimaryStorageStatus.Disconnected);
                self = dbf.updateAndRefresh(self);
                evt.setErrorCode(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void connect(final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-iscsi-file-system-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("deploy-agent-to-iscsi-filesystem-primary-storage-%s", self.getUuid());
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                            trigger.next();
                            return;
                        }

                        SshFileMd5Checker checker = new SshFileMd5Checker();
                        checker.setTargetIp(getSelf().getHostname());
                        checker.setUsername(getSelf().getSshUsername());
                        checker.setPassword(getSelf().getSshPassword());
                        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/iscsi/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/iscsi/%s", IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PACKAGE_NAME), true).getAbsolutePath(),
                                String.format("/var/lib/zstack/iscsi/package/%s", IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PACKAGE_NAME));

                        AnsibleRunner runner = new AnsibleRunner();
                        runner.installChecker(checker);
                        runner.setPassword(getSelf().getSshPassword());
                        runner.setUsername(getSelf().getSshUsername());
                        runner.setTargetIp(getSelf().getHostname());
                        runner.setAgentPort(IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PORT);
                        runner.setPlayBookName(IscsiFileSystemBackendPrimaryStorageGlobalProperty.ANSIBLE_PLAYBOOK_NAME);
                        runner.putArgument("pkg_iscsiagent", IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PACKAGE_NAME);
                        runner.run(new Completion(trigger) {
                            @Override
                            public void success() {
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
                    String __name__ = String.format("init-iscsi-filesystem-primary-storage-%s", self.getUuid());

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        InitCmd cmd = new InitCmd();
                        cmd.setRootFolderPath(getSelf().getUrl());
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.INIT_PATH), cmd, new JsonAsyncRESTCallback<InitRsp>() {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (ret.isSuccess()) {
                                    new PrimaryStorageCapacityUpdater(self.getUuid()).update(
                                            ret.getTotalCapacity(), ret.getAvailableCapacity(), ret.getTotalCapacity(), ret.getAvailableCapacity()
                                    );

                                    trigger.next();
                                } else {
                                    trigger.fail(errf.stringToOperationError(ret.getError()));
                                }
                            }

                            @Override
                            public Class<InitRsp> getReturnClass() {
                                return InitRsp.class;
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
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
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof IscsiBtrfsPrimaryStorageAsyncCallMsg) {
            handle((IscsiBtrfsPrimaryStorageAsyncCallMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        final BackupStorageInventory bs = msg.getBackupStorage();
        IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()));
        final String bsIntallPath = makeVolumeSnapshotInstallPathOnBackupStorage(bs.getUuid(), msg.getSnapshot().getUuid());
        mediator.uploadBits(getSelfInventory(), bs, bsIntallPath, msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                reply.setBackupStorageInstallPath(bsIntallPath);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(final IscsiBtrfsPrimaryStorageAsyncCallMsg msg) {
        final IscsiBtrfsPrimaryStorageAsyncCallReply reply = new IscsiBtrfsPrimaryStorageAsyncCallReply();

        restf.asyncJsonPost(makeHttpUrl(msg.getPath()), msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(LinkedHashMap ret) {
                reply.setResponse(ret);
                bus.reply(msg, reply);
            }

            @Override
            public Class<LinkedHashMap> getReturnClass() {
                return LinkedHashMap.class;
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-%s-from-snapshot", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            String installPath = makeDataVolumePath(msg.getVolumeUuid());
            SnapshotDownloadInfo snapshotInfo;

            @Override
            public void setup() {
                DebugUtils.Assert(msg.getSnapshots().size() == 1, String.format("why more[%s] than one snapshot???", msg.getSnapshots().size()));
                snapshotInfo = msg.getSnapshots().get(0);

                if (msg.isNeedDownload()) {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            BackupStorageVO bsvo = dbf.findByUuid(snapshotInfo.getBackupStorageUuid(), BackupStorageVO.class);
                            IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsvo.getType()));
                            mediator.downloadBits(getSelfInventory(), BackupStorageInventory.valueOf(bsvo), snapshotInfo.getBackupStorageInstallPath(), installPath, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            CreateSubVolumeCmd cmd = new CreateSubVolumeCmd();
                            cmd.setSrc(snapshotInfo.getSnapshot().getPrimaryStorageInstallPath());
                            cmd.setDst(makeDataVolumeSubvolumePath(msg.getVolumeUuid()));
                            restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_SUBVOLUME), cmd, new JsonAsyncRESTCallback<CreateSubVolumeRsp>() {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(CreateSubVolumeRsp ret) {
                                    if (!ret.isSuccess()) {
                                        trigger.fail(errf.stringToOperationError(ret.getError()));
                                    } else {
                                        installPath = ret.getPath();
                                        trigger.next();
                                    }
                                }

                                @Override
                                public Class<CreateSubVolumeRsp> getReturnClass() {
                                    return CreateSubVolumeRsp.class;
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(msg) {
                        @Override
                        public void handle(Map data) {
                            IscsiVolumePath ipath = new IscsiVolumePath();
                            ipath.setHostname(getSelf().getHostname());
                            ipath.setInstallPath(installPath);
                            ipath.setTarget(makeIscsiTargetName(msg.getVolumeUuid()));
                            reply.setInstallPath(ipath.assemble());
                            reply.setSize(snapshotInfo.getSnapshot().getSize());
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
            }
        }).start();
    }

    private void handle(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-template-%s-form-volume-snapshot", msg.getImageUuid()));
        chain.then(new ShareFlow() {
            String primaryStorageInstallPath;
            List<CreateTemplateFromVolumeSnapshotResult> results = new ArrayList<CreateTemplateFromVolumeSnapshotResult>();
            SnapshotDownloadInfo snapshotInfo;

            @Override
            public void setup() {
                DebugUtils.Assert(msg.getSnapshotsDownloadInfo().size()==1, String.format("why more[%s] than one snapshot???", msg.getSnapshotsDownloadInfo().size()));
                snapshotInfo = msg.getSnapshotsDownloadInfo().get(0);

                if (msg.isNeedDownload()) {
                    flow(new Flow() {
                        String __name__ = "download-snapshots-to-primary-storage";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            primaryStorageInstallPath = makeSnapshotWorkSpaceInstallPath(snapshotInfo.getSnapshot().getUuid());
                            BackupStorageVO bsvo = dbf.findByUuid(snapshotInfo.getBackupStorageUuid(), BackupStorageVO.class);
                            BackupStorageInventory bs = BackupStorageInventory.valueOf(bsvo);
                            DebugUtils.Assert(bs != null, "bs cannot be null");
                            IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()));
                            mediator.downloadBits(getSelfInventory(), bs, snapshotInfo.getBackupStorageInstallPath(), primaryStorageInstallPath, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (primaryStorageInstallPath != null) {
                                DeleteBitsCmd cmd = new DeleteBitsCmd();
                                cmd.setInstallPath(primaryStorageInstallPath);
                                restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(trigger) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        logger.warn(String.format("failed to delete %s on btrfs iscsi primary storage[uuid:%s], %s. Continue to rollback",
                                                primaryStorageInstallPath, self.getUuid(), err));
                                        trigger.rollback();
                                    }

                                    @Override
                                    public void success(DeleteBitsRsp ret) {
                                        if (!ret.isSuccess()) {
                                            logger.warn(String.format("failed to delete %s on btrfs iscsi primary storage[uuid:%s], %s. Continue to rollback",
                                                    primaryStorageInstallPath, self.getUuid(), ret.getError()));
                                        } else {
                                            reportCapacity(ret);
                                        }
                                        trigger.rollback();
                                    }

                                    @Override
                                    public Class<DeleteBitsRsp> getReturnClass() {
                                        return DeleteBitsRsp.class;
                                    }
                                });
                            }
                        }
                    });
                } else {
                    primaryStorageInstallPath = snapshotInfo.getSnapshot().getPrimaryStorageInstallPath();
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "upload-template-from-primary-storage-to-backup-storage";

                    boolean success = false;
                    List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        upload(msg.getBackupStorage().iterator(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    private void upload(final Iterator<BackupStorageInventory> iterator, final Completion completion) {
                        if (!iterator.hasNext()) {
                            if (success) {
                                completion.success();
                            } else {
                                completion.fail(errf.stringToOperationError(String.format("uploading failed on all backup storage. An error list is %s", JSONObjectUtil.toJsonString(errorCodes))));
                            }

                            return;
                        }

                        final BackupStorageInventory bs = iterator.next();
                        final String bsInstallPath = makeRootVolumeTemplateInstallPathOnBackupStorage(bs.getUuid(), msg.getImageUuid());
                        IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()));
                        mediator.uploadBits(getSelfInventory(), bs, bsInstallPath, primaryStorageInstallPath, new Completion(completion) {
                            @Override
                            public void success() {
                                success = true;
                                CreateTemplateFromVolumeSnapshotResult result = new CreateTemplateFromVolumeSnapshotResult();
                                result.setInstallPath(bsInstallPath);
                                result.setBackupStorageUuid(bs.getUuid());
                                results.add(result);
                                upload(iterator, completion);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to upload %s from btrfs iscsi primary storage[uuid:%s] to backup storage[uuid: %s], %s. Continue to upload others",
                                        primaryStorageInstallPath, bs.getUuid(), self.getUuid(), errorCode));
                                errorCodes.add(errorCode);
                                upload(iterator, completion);
                            }
                        });
                    }
                });

                if (msg.isNeedDownload()) {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DeleteBitsCmd cmd = new DeleteBitsCmd();
                            cmd.setInstallPath(primaryStorageInstallPath);
                            restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(trigger) {
                                @Override
                                public void fail(ErrorCode err) {
                                    //TODO: cleanup
                                    logger.warn(String.format("failed to delete %s on btrfs iscsi primary storage[uuid:%s], %s. Continue to rollback",
                                            primaryStorageInstallPath, self.getUuid(), err));
                                    trigger.next();
                                }

                                @Override
                                public void success(DeleteBitsRsp ret) {
                                    //TODO: cleanup
                                    if (!ret.isSuccess()) {
                                        logger.warn(String.format("failed to delete %s on btrfs iscsi primary storage[uuid:%s], %s. Continue to rollback",
                                                primaryStorageInstallPath, self.getUuid(), ret.getError()));
                                    } else {
                                        reportCapacity(ret);
                                    }
                                    trigger.next();
                                }

                                @Override
                                public Class<DeleteBitsRsp> getReturnClass() {
                                    return DeleteBitsRsp.class;
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setResults(results);
                        reply.setSize(snapshotInfo.getSnapshot().getSize());
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

    private void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        final IscsiVolumePath path = new IscsiVolumePath(msg.getVolume().getInstallPath());
        path.disassemble();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-%s-to-snapshot-%s", msg.getVolume().getUuid(), msg.getSnapshot().getUuid()));
        chain.then(new ShareFlow() {
            String newVolumePath;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-a-subvolume-from-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateSubVolumeCmd cmd = new CreateSubVolumeCmd();
                        cmd.setSrc(msg.getSnapshot().getPrimaryStorageInstallPath());
                        cmd.setDst(makeRootVolumeBySnapshotPath(msg.getVolume().getUuid(), msg.getSnapshot().getUuid()));
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_SUBVOLUME), cmd, new JsonAsyncRESTCallback<CreateSubVolumeRsp>(msg) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CreateSubVolumeRsp ret) {
                                if (!ret.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(ret.getError()));
                                } else {
                                    newVolumePath = ret.getPath();
                                    trigger.next();
                                }
                            }

                            @Override
                            public Class<CreateSubVolumeRsp> getReturnClass() {
                                return CreateSubVolumeRsp.class;
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (newVolumePath == null) {
                            trigger.rollback();
                            return;
                        }

                        DeleteBitsCmd cmd = new DeleteBitsCmd();
                        cmd.setInstallPath(newVolumePath);
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                logger.warn(String.format("failed to delete subvolume[%s], %s. Continue to rollback", newVolumePath, err));
                                trigger.rollback();
                            }

                            @Override
                            public void success(DeleteBitsRsp ret) {
                                if (!ret.isSuccess()) {
                                    logger.warn(String.format("failed to delete subvolume[%s], %s. Continue to rollback", newVolumePath, ret.getError()));
                                } else {
                                    reportCapacity(ret);
                                }
                                trigger.rollback();
                            }

                            @Override
                            public Class<DeleteBitsRsp> getReturnClass() {
                                return DeleteBitsRsp.class;
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-old-subvolume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DeleteBitsCmd cmd = new DeleteBitsCmd();
                        cmd.setInstallPath(path.getInstallPath());
                        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                logger.warn(String.format("failed to delete old subvolume[%s], %s. Continue to proceed", path.getInstallPath(), err));
                                trigger.next();
                            }

                            @Override
                            public void success(DeleteBitsRsp ret) {
                                if (!ret.isSuccess()) {
                                    logger.warn(String.format("failed to delete old subvolume[%s], %s. Continue to proceed", path.getInstallPath(), ret.getError()));
                                } else {
                                    reportCapacity(ret);
                                }
                                trigger.next();
                            }

                            @Override
                            public Class<DeleteBitsRsp> getReturnClass() {
                                return DeleteBitsRsp.class;
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        path.setInstallPath(newVolumePath);
                        reply.setNewVolumeInstallPath(path.assemble());
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

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.primaryStorageInstallPath);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getSnapshot().getUuid());
        String installPath = q.findValue();

        DeleteBitsCmd cmd = new DeleteBitsCmd();
        cmd.setInstallPath(installPath);
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteBitsRsp ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                } else {
                    reportCapacity(ret);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteBitsRsp> getReturnClass() {
                return DeleteBitsRsp.class;
            }
        });
    }

    private void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    private void handle(final TakeSnapshotMsg msg) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        final VolumeSnapshotStruct struct = msg.getStruct();
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.installPath);
        q.add(VolumeVO_.uuid, Op.EQ, struct.getCurrent().getVolumeUuid());
        String volumePath = q.findValue();
        IscsiVolumePath path = new IscsiVolumePath(volumePath);
        path.disassemble();

        CreateSubVolumeCmd cmd = new CreateSubVolumeCmd();
        cmd.setSrc(path.getInstallPath());
        cmd.setDst(makeVolumeSnapshotPath(struct.getCurrent().getVolumeUuid(), struct.getCurrent().getUuid()));
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.CREATE_SUBVOLUME), cmd, new JsonAsyncRESTCallback<CreateSubVolumeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateSubVolumeRsp ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                } else {
                    VolumeSnapshotInventory inv = struct.getCurrent();
                    inv.setSize(ret.getSize());
                    inv.setPrimaryStorageUuid(self.getUuid());
                    inv.setPrimaryStorageInstallPath(ret.getPath());
                    inv.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                    inv.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                    reply.setInventory(inv);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<CreateSubVolumeRsp> getReturnClass() {
                return CreateSubVolumeRsp.class;
            }
        });
    }

    @Override
    protected PrimaryStorageVO updatePrimaryStorage(APIUpdatePrimaryStorageMsg pmsg) {
        APIUpdateIscsiFileSystemBackendPrimaryStorageMsg msg = (APIUpdateIscsiFileSystemBackendPrimaryStorageMsg) pmsg;
        PrimaryStorageVO vo = super.updatePrimaryStorage(msg);
        self = vo == null ? getSelf() : vo;

        boolean update = false;
        if (msg.getChapUsername() != null) {
            getSelf().setChapUsername(msg.getChapUsername());
            update = true;
        }
        if (msg.getChapPassword() != null) {
            getSelf().setChapPassword(msg.getChapPassword());
            update = true;
        }

        if ((getSelf().getChapUsername() != null && getSelf().getChapPassword() == null)
                || getSelf().getChapUsername() == null && getSelf().getChapPassword() != null) {
            throw new OperationFailureException(errf.stringToInvalidArgumentError(String.format("chapUsername and chapPassword must be both null or not null; you can not set chapUsername without setting chapPassword, vice versa.")));
        }

        if (msg.getSshUsername() != null) {
            getSelf().setSshUsername(msg.getSshUsername());
            update = true;
        }
        if (msg.getSshPassword() != null) {
            getSelf().setSshPassword(msg.getSshPassword());
            update = true;
        }

        return update ? self : null;
    }
}
