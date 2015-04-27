package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.*;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageManager;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

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

    private String makeRootVolumePath(String volumeUuid) {
        return PathUtil.join(self.getUrl(), "rootVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s", volumeUuid), String.format("%s.img", volumeUuid));
    }

    private String makeDataVolumePath(String volumeUuid) {
        return PathUtil.join(self.getUrl(), "dataVolumes", String.format("acct-%s", acntMgr.getOwnerAccountUuidOfResource(volumeUuid)),
                String.format("vol-%s", volumeUuid), String.format("%s.img", volumeUuid));
    }

    private String makeIscsiVolumePath(String target, String volumePath) {
        IscsiVolumePath path = new IscsiVolumePath();
        path.setInstallPath(volumePath);
        path.setHostname(getSelf().getHostname());
        path.setTarget(target);
        return path.assemble();
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
        restf.asyncJsonPost(makeHttpUrl(IscsiBtrfsPrimaryStorageConstants.DELETE_BITS_EXISTENCE), cmd, new JsonAsyncRESTCallback<DeleteBitsRsp>() {
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
                            thdf.chainSubmit(new ChainTask(trigger) {
                                @Override
                                public String getSyncSignature() {
                                    return getName();
                                }

                                @Override
                                public void run(final SyncTaskChain chain) {
                                    Completion completion = new Completion(chain, trigger) {
                                        @Override
                                        public void success() {
                                            trigger.next();
                                            chain.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                            chain.next();
                                        }
                                    };

                                    SimpleQuery<ImageCacheVO> query = dbf.createQuery(ImageCacheVO.class);
                                    query.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                                    query.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
                                    ImageCacheVO cvo = query.find();
                                    if (cvo != null) {
                                        checkCache(cvo, completion);
                                    } else {
                                        downloadToCache(null, completion);
                                    }
                                }

                                @Override
                                public String getName() {
                                    return String.format("download-image-%s-to-iscsi-primary-storage-%s", image.getUuid(), self.getUuid());
                                }
                            });

                        }

                        private void downloadToCache(final ImageCacheVO cvo, final Completion completion) {
                            IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsinv.getType()),
                                    VolumeFormat.getMasterHypervisorTypeByVolumeFormat(image.getFormat()));

                            imagePathInCache = makePathInImageCache(image.getUuid());
                            mediator.downloadBits(getSelfInventory(), bsinv,
                                    ispec.getSelectedBackupStorage().getInstallPath(),  imagePathInCache, new Completion() {
                                        @Override
                                        public void success() {
                                            if (cvo == null) {
                                                ImageCacheVO vo = new ImageCacheVO();
                                                vo.setImageUuid(image.getUuid());
                                                vo.setInstallUrl(imagePathInCache);
                                                vo.setMediaType(ImageMediaType.valueOf(image.getMediaType()));
                                                vo.setPrimaryStorageUuid(self.getUuid());
                                                vo.setSize(image.getSize());
                                                vo.setState(ImageCacheState.ready);
                                                vo.setMd5sum("not calculated");
                                                dbf.persist(vo);
                                            } else {
                                                cvo.setInstallUrl(imagePathInCache);
                                                dbf.update(cvo);
                                            }

                                            completion.success();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            completion.fail(errorCode);
                                        }
                                    });
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
                                            imagePathInCache = cvo.getInstallUrl();
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
                        public void rollback(FlowTrigger trigger, Map data) {
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
            psMgr.sendCapacityReportMessage(ret.getTotalCapacity(), ret.getAvailableCapacity(), self.getUuid());
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

        IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsinv.getType()),
                VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat()));

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
                reply.setFormat(KVMConstant.RAW_FORMAT_STRING);
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

        final IscsiFileSystemBackendPrimaryToBackupStorageMediator mediator = factory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bsinv.getType()),
                VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat()));

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
                    public void rollback(final FlowTrigger trigger, Map data) {
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
                        reply.setFormat(KVMConstant.RAW_FORMAT_STRING);
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
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ConnectPrimaryStorageMsg) {
            handle((ConnectPrimaryStorageMsg)msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }
    
    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectPrimaryStorageMsg) {
            handle((APIReconnectPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIReconnectPrimaryStorageMsg msg) {
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
                        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/iscsi/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/iscsi/%s", IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PACKAGE_NAME), true).getAbsolutePath(),
                                String.format("/var/lib/zstack/iscsi/%s", IscsiFileSystemBackendPrimaryStorageGlobalProperty.AGENT_PACKAGE_NAME));

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
                                    reportCapacity(ret);
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

    private void handle(final ConnectPrimaryStorageMsg msg) {
        final ConnectPrimaryStorageReply reply = new ConnectPrimaryStorageReply();
        connect(new Completion(msg) {
            @Override
            public void success() {
                reply.setConnected(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
