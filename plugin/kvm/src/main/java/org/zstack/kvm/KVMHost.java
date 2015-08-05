package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.compute.host.HostBase;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec.IsoSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMHost extends HostBase implements Host {
    private static final CLogger logger = Utils.getLogger(KVMHost.class);

    @Autowired
    private KVMHostFactory factory;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private KVMExtensionEmitter extEmitter;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private KVMHostContext context;

    // ///////////////////// REST URL //////////////////////////
    private String baseUrl;
    private String connectPath;
    private String pingPath;
    private String checkPhysicalNetworkInterfacePath;
    private String startVmPath;
    private String stopVmPath;
    private String rebootVmPath;
    private String destroyVmPath;
    private String attachDataVolumePath;
    private String detachDataVolumePath;
    private String echoPath;
    private String attachNicPath;
    private String detachNicPath;
    private String migrateVmPath;
    private String snapshotPath;
    private String mergeSnapshotPath;
    private String hostFactPath;

    private String agentPackageName = KVMGlobalProperty.AGENT_PACKAGE_NAME;

    KVMHost(KVMHostVO self, KVMHostContext context) {
        super(self);

        this.context = context;
        baseUrl = context.getBaseUrl();

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_CONNECT_PATH);
        connectPath = ub.build().toUriString();
        
        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_PING_PATH);
        pingPath = ub.build().toUriString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH);
        checkPhysicalNetworkInterfacePath = ub.build().toUriString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_START_VM_PATH);
        startVmPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_STOP_VM_PATH);
        stopVmPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_REBOOT_VM_PATH);
        rebootVmPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_DESTROY_VM_PATH);
        destroyVmPath = ub.build().toString();
        
        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_ATTACH_VOLUME);
        attachDataVolumePath = ub.build().toString();
        
        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_DETACH_VOLUME);
        detachDataVolumePath = ub.build().toString();
        
        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_ECHO_PATH);
        echoPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_ATTACH_NIC_PATH);
        attachNicPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_DETACH_NIC_PATH);
        detachNicPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_MIGRATE_VM_PATH);
        migrateVmPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH);
        snapshotPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_MERGE_SNAPSHOT_PATH);
        mergeSnapshotPath = ub.build().toString();

        ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(KVMConstant.KVM_HOST_FACT_PATH);
        hostFactPath = ub.build().toString();
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        super.handleApiMessage(msg);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof CheckNetworkPhysicalInterfaceMsg) {
            handle((CheckNetworkPhysicalInterfaceMsg) msg);
        } else if (msg instanceof StartVmOnHypervisorMsg) {
            handle((StartVmOnHypervisorMsg) msg);
        } else if (msg instanceof CreateVmOnHypervisorMsg) {
            handle((CreateVmOnHypervisorMsg) msg);
        } else if (msg instanceof StopVmOnHypervisorMsg) {
            handle((StopVmOnHypervisorMsg) msg);
        } else if (msg instanceof RebootVmOnHypervisorMsg) {
            handle((RebootVmOnHypervisorMsg) msg);
        } else if (msg instanceof DestroyVmOnHypervisorMsg) {
            handle((DestroyVmOnHypervisorMsg) msg);
        } else if (msg instanceof AttachVolumeToVmOnHypervisorMsg) {
            handle((AttachVolumeToVmOnHypervisorMsg) msg);
        } else if (msg instanceof DetachVolumeFromVmOnHypervisorMsg) {
            handle((DetachVolumeFromVmOnHypervisorMsg) msg);
        } else if (msg instanceof VmAttachNicOnHypervisorMsg) {
            handle((VmAttachNicOnHypervisorMsg) msg);
        } else if (msg instanceof MigrateVmOnHypervisorMsg) {
            handle((MigrateVmOnHypervisorMsg) msg);
        } else if (msg instanceof TakeSnapshotOnHypervisorMsg) {
            handle((TakeSnapshotOnHypervisorMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnKvmMsg) {
            handle((MergeVolumeSnapshotOnKvmMsg) msg);
        } else if (msg instanceof KVMHostAsyncHttpCallMsg) {
            handle((KVMHostAsyncHttpCallMsg) msg);
        } else if (msg instanceof KVMHostSyncHttpCallMsg) {
            handle((KVMHostSyncHttpCallMsg) msg);
        } else if (msg instanceof DetachNicFromVmOnHypervisorMsg) {
            handle((DetachNicFromVmOnHypervisorMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final DetachNicFromVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                detachNic(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "detach-nic-on-kvm-host-" + self.getUuid();
            }


            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void detachNic(final DetachNicFromVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        final DetachNicFromVmOnHypervisorReply reply = new DetachNicFromVmOnHypervisorReply();
        NicTO to = completeNicInfo(msg.getNic());

        DetachNicCommand cmd = new DetachNicCommand();
        cmd.setVmUuid(msg.getVmInstanceUuid());
        cmd.setNic(to);
        restf.asyncJsonPost(detachNicPath, cmd, new JsonAsyncRESTCallback<DetachNicRsp>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(DetachNicRsp ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<DetachNicRsp> getReturnClass() {
                return DetachNicRsp.class;
            }
        });
    }

    private void handle(final KVMHostSyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                executeSyncHttpCall(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("execute-sync-http-call-on-kvm-host-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void executeSyncHttpCall(KVMHostSyncHttpCallMsg msg, NoErrorCompletion completion) {
        if (!msg.isNoStatusCheck()) {
            checkStatus();
        }
        String url = buildUrl(msg.getPath());
        LinkedHashMap rsp = restf.syncJsonPost(url, msg.getCommand(), LinkedHashMap.class);
        KVMHostSyncHttpCallReply reply = new KVMHostSyncHttpCallReply();
        reply.setResponse(rsp);
        bus.reply(msg, reply);
        completion.done();
    }

    private void handle(final KVMHostAsyncHttpCallMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                executeAsyncHttpCall(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("execute-async-http-call-on-kvm-host-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private String buildUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(KVMGlobalProperty.AGENT_URL_SCHEME);
        ub.host(self.getManagementIp());
        ub.port(KVMGlobalProperty.AGENT_PORT);
        if (!"".equals(KVMGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(KVMGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(path);
        return ub.build().toUriString();
    }

    private void executeAsyncHttpCall(final KVMHostAsyncHttpCallMsg msg, final NoErrorCompletion completion) {
        if (!msg.isNoStatusCheck()) {
            checkStatus();
        }

        String url = buildUrl(msg.getPath());
        restf.asyncJsonPost(url, msg.getCommand(), new JsonAsyncRESTCallback<LinkedHashMap>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply();
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(LinkedHashMap ret) {
                KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply();
                reply.setResponse(ret);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<LinkedHashMap> getReturnClass() {
                return LinkedHashMap.class;
            }
        }, TimeUnit.SECONDS, msg.getCommandTimeout());
    }

    private void handle(final MergeVolumeSnapshotOnKvmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                mergeVolumeSnapshot(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("merge-volume-snapshot-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void mergeVolumeSnapshot(final MergeVolumeSnapshotOnKvmMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();

        final MergeVolumeSnapshotOnKvmReply reply = new MergeVolumeSnapshotOnKvmReply();

        VolumeInventory volume = msg.getTo();

        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            if (state != VmInstanceState.Stopped && state != VmInstanceState.Running) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("cannot do volume snapshot merge when vm[uuid:%s] is in state of %s. The operation is only allowed when vm is Running or Stopped",
                                volume.getUuid(), state)
                ));
            }

            if (state == VmInstanceState.Running) {
                String libvirtVersion = KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(self.getUuid(), KVMSystemTags.LIBVIRT_VERSION_TOKEN);
                if (KVMConstant.MIN_LIBVIRT_LIVE_BLOCK_COMMIT_VERSION.compareTo(libvirtVersion) > 0) {
                    throw new OperationFailureException(errf.stringToOperationError(
                            String.format("live volume snapshot merge needs libvirt version greater than %s, current libvirt version is %s. Please stop vm and redo the operation or detach the volume if it's data volume",
                                    KVMConstant.MIN_LIBVIRT_LIVE_BLOCK_COMMIT_VERSION, libvirtVersion)
                    ));
                }
            }
        }

        VolumeSnapshotInventory snapshot = msg.getFrom();
        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setFullRebase(msg.isFullRebase());
        cmd.setDestPath(volume.getInstallPath());
        cmd.setSrcPath(snapshot.getPrimaryStorageInstallPath());
        cmd.setVmUuid(volume.getVmInstanceUuid());
        cmd.setDeviceId(volume.getDeviceId());
        restf.asyncJsonPost(mergeSnapshotPath, cmd, new JsonAsyncRESTCallback<MergeSnapshotRsp>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(MergeSnapshotRsp ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<MergeSnapshotRsp> getReturnClass() {
                return MergeSnapshotRsp.class;
            }
        });
    }

    private void handle(final TakeSnapshotOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                takeSnapshot(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("take-snapshot-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void takeSnapshot(final TakeSnapshotOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();

        final TakeSnapshotOnHypervisorReply reply = new TakeSnapshotOnHypervisorReply();
        TakeSnapshotCmd cmd = new TakeSnapshotCmd();

        if (msg.getVmUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, msg.getVmUuid());
            VmInstanceState vmState = q.findValue();
            if (vmState != VmInstanceState.Running && vmState != VmInstanceState.Stopped) {
                throw new OperationFailureException(errf.stringToOperationError(
                        String.format("vm[uuid:%s] is not Running or Stopped, current state[%s]", msg.getVmUuid(),
                                vmState)
                ));
            }

            if (!HostSystemTags.LIVE_SNAPSHOT.hasTag(self.getUuid())) {
                if (vmState != VmInstanceState.Stopped) {
                    throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.NO_CAPABILITY_ERROR,
                            String.format("kvm host[uuid:%s, name:%s, ip:%s] doesn't not support live snapshot. please stop vm[uuid:%s] and try again",
                                    self.getUuid(), self.getName(), self.getManagementIp(), msg.getVmUuid())
                    ));
                }
            }

            cmd.setVmUuid(msg.getVmUuid());
            cmd.setDeviceId(msg.getVolume().getDeviceId());
        }

        cmd.setVolumeInstallPath(msg.getVolume().getInstallPath());
        cmd.setInstallPath(msg.getInstallPath());
        cmd.setFullSnapshot(msg.isFullSnapshot());
        restf.asyncJsonPost(snapshotPath, cmd, new JsonAsyncRESTCallback<TakeSnapshotResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(TakeSnapshotResponse ret) {
                if (ret.isSuccess()) {
                    reply.setNewVolumeInstallPath(ret.getNewVolumeInstallPath());
                    reply.setSnapshotInstallPath(ret.getSnapshotInstallPath());
                    reply.setSize(ret.getSize());
                } else {
                    reply.setError(errf.stringToOperationError(ret.getError()));
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<TakeSnapshotResponse> getReturnClass() {
                return TakeSnapshotResponse.class;
            }
        });
    }

    private void migrateVm(final Iterator<String[]> it, final Completion completion) {
        String hostIp = null;
        String vmUuid = null;
        synchronized (it) {
            if (!it.hasNext()) {
                completion.success();
                return;
            }

            String[] arr = it.next();
            vmUuid =  arr[0];
            hostIp = arr[1];
        }

        MigrateVmCmd cmd = new MigrateVmCmd();
        cmd.setDestHostIp(hostIp);
        cmd.setVmUuid(vmUuid);
        final String fvmuuid = vmUuid;
        final String destIp = hostIp;
        restf.asyncJsonPost(migrateVmPath, cmd, new JsonAsyncRESTCallback<MigrateVmResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(MigrateVmResponse ret) {
                if (!ret.isSuccess()) {
                    ErrorCode err = errf.instantiateErrorCode(HostErrors.FAILED_TO_MIGRATE_VM_ON_HYPERVISOR,
                            String.format("failed to migrate vm[uuid:%s] from kvm host[uuid:%s, ip:%s] to dest host[ip:%s], %s",
                                    fvmuuid, self.getUuid(), self.getManagementIp(), destIp, ret.getError())
                    );
                    completion.fail(err);
                } else {
                    String info = String.format("successfully migrated vm[uuid:%s] from kvm host[uuid:%s, ip:%s] to dest host[ip:%s]",
                            fvmuuid, self.getUuid(), self.getManagementIp(), destIp);
                    logger.debug(info);
                    migrateVm(it, completion);
                }
            }

            @Override
            public Class<MigrateVmResponse> getReturnClass() {
                return MigrateVmResponse.class;
            }
        });
    }

    private void handle(final MigrateVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                migrateVm(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("migrate-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void migrateVm(final MigrateVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStatus();

        List<String[]> lst = new ArrayList<String[]>();
        lst.add(new String[] {msg.getVmInventory().getUuid(), msg.getDestHostInventory().getManagementIp()});
        final MigrateVmOnHypervisorReply reply = new MigrateVmOnHypervisorReply();
        migrateVm(lst.iterator(), new Completion(msg, completion) {
            @Override
            public void success() {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(final VmAttachNicOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                attachNic(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-nic-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void attachNic(final VmAttachNicOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();

        NicTO to = completeNicInfo(msg.getNicInventory());

        final VmAttachNicOnHypervisorReply reply = new VmAttachNicOnHypervisorReply();
        AttachNicCommand cmd = new AttachNicCommand();
        cmd.setVmUuid(msg.getNicInventory().getVmInstanceUuid());
        cmd.setNic(to);
        restf.asyncJsonPost(attachNicPath, cmd, new JsonAsyncRESTCallback<AttachNicResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(AttachNicResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(errf.stringToTimeoutError(String.format("failed to attach nic[uuid:%s, vm:%s] on kvm host[uuid:%s, ip:%s]," +
                                    "because %s", msg.getNicInventory().getUuid(), msg.getNicInventory().getVmInstanceUuid(),
                            self.getUuid(), self.getManagementIp(), ret.getError())));
                }

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<AttachNicResponse> getReturnClass() {
                return AttachNicResponse.class;
            }
        }, TimeUnit.SECONDS, 300);
    }


    private void handle(final DetachVolumeFromVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                detachVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("detach-volume-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });

    }

    private void detachVolume(final DetachVolumeFromVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();

        VolumeTO to = new VolumeTO();
        final VolumeInventory vol = msg.getInventory();
        final VmInstanceInventory vm = msg.getVmInventory();
        to.setInstallPath(vol.getInstallPath());
        to.setDeviceId(vol.getDeviceId());
        to.setDeviceType(getVolumeTOType(vol));
        to.setVolumeUuid(vol.getUuid());
        to.setUseVirtio(ImagePlatform.Linux.toString().equals(vm.getPlatform()) || ImagePlatform.Paravirtualization.toString().equals(vm.getPlatform())
                || ImagePlatform.Windows.toString().equals(vm.getPlatform()));

        final DetachVolumeFromVmOnHypervisorReply reply = new DetachVolumeFromVmOnHypervisorReply();
        final DetachDataVolumeCmd cmd = new DetachDataVolumeCmd();
        cmd.setVolume(to);
        cmd.setVmUuid(vm.getUuid());
        extEmitter.beforeDetachVolume((KVMHostInventory)getSelfInventory(), vm, vol, cmd);
        restf.asyncJsonPost(detachDataVolumePath, cmd, new JsonAsyncRESTCallback<DetachDataVolumeResponse>(msg, completion) {

            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                extEmitter.detachVolumeFailed((KVMHostInventory) getSelfInventory(), vm, vol, cmd, err);
                completion.done();
            }

            @Override
            public void success(DetachDataVolumeResponse ret) {
                if (!ret.isSuccess()) {
                    String err = String.format("failed to detach data volume[uuid:%s, installPath:%s] from vm[uuid:%s, name:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            vol.getUuid(), vol.getInstallPath(), vm.getUuid(), vm.getName(), getSelf().getUuid(), getSelf().getManagementIp(), ret.getError());
                    logger.warn(err);
                    reply.setError(errf.stringToOperationError(err));
                    extEmitter.detachVolumeFailed((KVMHostInventory) getSelfInventory(), vm, vol, cmd, reply.getError());
                } else {
                    extEmitter.afterDetachVolume((KVMHostInventory) getSelfInventory(), vm, vol, cmd);
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<DetachDataVolumeResponse> getReturnClass() {
                return DetachDataVolumeResponse.class;
            }
        }, TimeUnit.SECONDS, 180);
    }

    private void handle(final AttachVolumeToVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                attachVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("attach-volume-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void attachVolume(final AttachVolumeToVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();

        VolumeTO to = new VolumeTO();
        final VolumeInventory vol = msg.getInventory();
        final VmInstanceInventory vm = msg.getVmInventory();
        to.setInstallPath(vol.getInstallPath());
        to.setDeviceId(vol.getDeviceId());
        to.setDeviceType(getVolumeTOType(vol));
        to.setVolumeUuid(vol.getUuid());
        to.setUseVirtio(ImagePlatform.Linux.toString().equals(vm.getPlatform()) || ImagePlatform.Paravirtualization.toString().equals(vm.getPlatform()) || ImagePlatform.Windows.toString().equals(vm.getPlatform()));

        final AttachVolumeToVmOnHypervisorReply reply = new AttachVolumeToVmOnHypervisorReply();
        final AttachDataVolumeCmd cmd = new AttachDataVolumeCmd();
        cmd.setVolume(to);
        cmd.setVmUuid(msg.getVmInventory().getUuid());
        extEmitter.beforeAttachVolume((KVMHostInventory)getSelfInventory(), vm, vol, cmd);
        restf.asyncJsonPost(attachDataVolumePath, cmd, new JsonAsyncRESTCallback<AttachDataVolumeResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                extEmitter.attachVolumeFailed((KVMHostInventory) getSelfInventory(), vm, vol, cmd, err);
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(AttachDataVolumeResponse ret) {
                if (!ret.isSuccess()) {
                    String err = String.format("failed to attach data volume[uuid:%s, installPath:%s] to vm[uuid:%s, name:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            vol.getUuid(), vol.getInstallPath(), vm.getUuid(), vm.getName(), getSelf().getUuid(), getSelf().getManagementIp(), ret.getError());
                    logger.warn(err);
                    reply.setError(errf.stringToOperationError(err));
                    extEmitter.attachVolumeFailed((KVMHostInventory) getSelfInventory(), vm, vol, cmd, reply.getError());
                } else {
                    extEmitter.afterAttachVolume((KVMHostInventory) getSelfInventory(), vm, vol, cmd);
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<AttachDataVolumeResponse> getReturnClass() {
                return AttachDataVolumeResponse.class;
            }

        }, TimeUnit.SECONDS, 180);
    }

    private void handle(final DestroyVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                destroyVm(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("destroy-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void destroyVm(final DestroyVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStatus();

        final VmInstanceInventory vminv = msg.getVmInventory();

        try {
            extEmitter.beforeDestroyVmOnKvm(KVMHostInventory.valueOf(getSelf()), vminv);
        } catch (KVMException e) {
            String err = String.format("failed to destroy vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s], because %s", vminv.getUuid(), vminv.getName(),
                    self.getUuid(), self.getManagementIp(), e.getMessage());
            logger.warn(err, e);
            throw new OperationFailureException(errf.stringToOperationError(err));
        }

        DestroyVmCmd cmd = new DestroyVmCmd();
        cmd.setUuid(vminv.getUuid());
        restf.asyncJsonPost(destroyVmPath, cmd, new JsonAsyncRESTCallback<DestroyVmResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                DestroyVmOnHypervisorReply reply = new DestroyVmOnHypervisorReply();
                reply.setError(err);
                extEmitter.destroyVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, reply.getError());
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(DestroyVmResponse ret) {
                DestroyVmOnHypervisorReply reply = new DestroyVmOnHypervisorReply();
                if (!ret.isSuccess()) {
                    String err = String.format("unable to destroy vm[uuid:%s,  name:%s] on kvm host [uuid:%s, ip:%s], because %s", vminv.getUuid(),
                            vminv.getName(), self.getUuid(), self.getManagementIp(), ret.getError());
                    reply.setError(errf.instantiateErrorCode(HostErrors.FAILED_TO_DESTROY_VM_ON_HYPERVISOR, err));
                    extEmitter.destroyVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, reply.getError());
                } else {
                    logger.debug(String.format("successfully destroyed vm[uuid:%s] on kvm host[uuid:%s]", vminv.getUuid(), self.getUuid()));
                    extEmitter.destroyVmOnKvmSuccess(KVMHostInventory.valueOf(getSelf()), vminv);
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<DestroyVmResponse> getReturnClass() {
                return DestroyVmResponse.class;
            }
        }, TimeUnit.SECONDS, TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout()));
    }

    private void handle(final RebootVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                rebootVm(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reboot-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });

    }

    private void rebootVm(final RebootVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStateAndStatus();
        final VmInstanceInventory vminv = msg.getVmInventory();

        try {
            extEmitter.beforeRebootVmOnKvm(KVMHostInventory.valueOf(getSelf()), vminv);
        } catch (KVMException e) {
            String err = String.format("failed to reboot vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s], because %s", vminv.getUuid(), vminv.getName(),
                    self.getUuid(), self.getManagementIp(), e.getMessage());
            logger.warn(err, e);
            throw new OperationFailureException(errf.stringToOperationError(err));
        }

        RebootVmCmd cmd = new RebootVmCmd();
        long timeout = TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout());
        cmd.setUuid(vminv.getUuid());
        cmd.setTimeout(timeout);
        restf.asyncJsonPost(rebootVmPath, cmd, new JsonAsyncRESTCallback<RebootVmResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                RebootVmOnHypervisorReply reply = new RebootVmOnHypervisorReply();
                reply.setError(err);
                extEmitter.rebootVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(RebootVmResponse ret) {
                RebootVmOnHypervisorReply reply = new RebootVmOnHypervisorReply();
                if (!ret.isSuccess()) {
                    String err = String.format("unable to reboot vm[uuid:%s, name:%s] on kvm host[uuid:%s, ip:%s], because %s", vminv.getUuid(),
                            vminv.getName(), self.getUuid(), self.getManagementIp(), ret.getError());
                    reply.setError(errf.instantiateErrorCode(HostErrors.FAILED_TO_REBOOT_VM_ON_HYPERVISOR, err));
                    extEmitter.rebootVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, reply.getError());
                } else {
                    extEmitter.rebootVmOnKvmSuccess(KVMHostInventory.valueOf(getSelf()), vminv);
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<RebootVmResponse> getReturnClass() {
                return RebootVmResponse.class;
            }

        }, TimeUnit.SECONDS, timeout);
    }

    private void handle(final StopVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                stopVm(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("stop-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void stopVm(final StopVmOnHypervisorMsg msg, final NoErrorCompletion completion) {
        checkStatus();
        final VmInstanceInventory vminv = msg.getVmInventory();

        try {
            extEmitter.beforeStopVmOnKvm(KVMHostInventory.valueOf(getSelf()), vminv);
        } catch (KVMException e) {
            String err = String.format("failed to stop vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s], because %s", vminv.getUuid(), vminv.getName(),
                    self.getUuid(), self.getManagementIp(), e.getMessage());
            logger.warn(err, e);
            throw new OperationFailureException(errf.stringToOperationError(err));
        }

        StopVmCmd cmd = new StopVmCmd();
        cmd.setUuid(vminv.getUuid());
        cmd.setTimeout(120);
        restf.asyncJsonPost(stopVmPath, cmd, new JsonAsyncRESTCallback<StopVmResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                StopVmOnHypervisorReply reply = new StopVmOnHypervisorReply();
                reply.setError(err);
                extEmitter.stopVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(StopVmResponse ret) {
                StopVmOnHypervisorReply reply = new StopVmOnHypervisorReply();
                if (!ret.isSuccess()) {
                    String err = String.format("unable to stop vm[uuid:%s,  name:%s] on kvm host[uuid:%s, ip:%s], because %s", vminv.getUuid(),
                            vminv.getName(), self.getUuid(), self.getManagementIp(), ret.getError());
                    reply.setError(errf.instantiateErrorCode(HostErrors.FAILED_TO_STOP_VM_ON_HYPERVISOR, err));
                    logger.warn(err);
                    extEmitter.stopVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), vminv, reply.getError());
                } else {
                    extEmitter.stopVmOnKvmSuccess(KVMHostInventory.valueOf(getSelf()), vminv);
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<StopVmResponse> getReturnClass() {
                return StopVmResponse.class;
            }

        }, TimeUnit.SECONDS, TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout()));
    }

    private void handle(final CreateVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                startVm(msg.getVmSpec(), msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("start-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    @Transactional
    private L2NetworkInventory getL2NetworkTypeFromL3NetworkUuid(String l3NetworkUuid) {
        String sql = "select l2 from L2NetworkVO l2 where l2.uuid = (select l3.l2NetworkUuid from L3NetworkVO l3 where l3.uuid = :l3NetworkUuid)";
        TypedQuery<L2NetworkVO> query = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
        query.setParameter("l3NetworkUuid", l3NetworkUuid);
        L2NetworkVO l2vo = query.getSingleResult();
        return L2NetworkInventory.valueOf(l2vo);
    }

    private NicTO completeNicInfo(VmNicInventory nic) {
        L2NetworkInventory l2inv = getL2NetworkTypeFromL3NetworkUuid(nic.getL3NetworkUuid());
        KVMCompleteNicInformationExtensionPoint extp = factory.getCompleteNicInfoExtension(L2NetworkType.valueOf(l2inv.getType()));
        NicTO to = extp.completeNicInformation(l2inv, nic);

        if (to.getUseVirtio() == null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.platform);
            q.add(VmInstanceVO_.uuid, Op.EQ, nic.getVmInstanceUuid());
            String platform = q.findValue();

            to.setUseVirtio(
                    platform.equals(ImagePlatform.Linux.toString()) || platform.equals(ImagePlatform.Paravirtualization.toString())
            );
        }

        return to;
    }

    private String getVolumeTOType(VolumeInventory vol) {
        return vol.getInstallPath().startsWith("iscsi") ? VolumeTO.ISCSI : VolumeTO.FILE;
    }

    private void startVm(final VmInstanceSpec spec, final NeedReplyMessage msg, final NoErrorCompletion completion) {
        checkStateAndStatus();
        final StartVmCmd cmd = new StartVmCmd();

        boolean virtio;
        String platform = spec.getVmInventory().getPlatform() == null ? spec.getImageSpec().getInventory().getPlatform() :
                spec.getVmInventory().getPlatform();

        if (ImagePlatform.Windows.toString().equals(platform)) {
            virtio = VmSystemTags.WINDOWS_VOLUME_ON_VIRTIO.hasTag(spec.getVmInventory().getUuid());
        } else {
            virtio = ImagePlatform.Linux.toString().equals(platform) || ImagePlatform.Paravirtualization.toString().equals(platform);
        }

        cmd.setVmName(spec.getVmInventory().getName());
        cmd.setVmUuid(spec.getVmInventory().getUuid());
        cmd.setCpuNum(spec.getVmInventory().getCpuNum());
        cmd.setCpuSpeed(spec.getVmInventory().getCpuSpeed());
        cmd.setMemory(spec.getVmInventory().getMemorySize());
        cmd.setUseVirtio(virtio);
        VolumeTO rootVolume = new VolumeTO();
        rootVolume.setInstallPath(spec.getDestRootVolume().getInstallPath());
        rootVolume.setDeviceId(spec.getDestRootVolume().getDeviceId());
        rootVolume.setDeviceType(getVolumeTOType(spec.getDestRootVolume()));
        rootVolume.setVolumeUuid(spec.getDestRootVolume().getUuid());
        rootVolume.setUseVirtio(virtio);
        cmd.setRootVolume(rootVolume);
        cmd.setTimeout(TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout()));
        List<VolumeTO> dataVolumes = new ArrayList<VolumeTO>(spec.getDestDataVolumes().size());
        for (VolumeInventory data : spec.getDestDataVolumes()) {
            VolumeTO v = new VolumeTO();
            v.setInstallPath(data.getInstallPath());
            v.setDeviceId(data.getDeviceId());
            v.setDeviceType(getVolumeTOType(data));
            v.setVolumeUuid(data.getUuid());
            v.setUseVirtio(virtio);
            dataVolumes.add(v);
        }
        cmd.setDataVolumes(dataVolumes);
        cmd.setVmInternalId(spec.getVmInventory().getInternalId());

        List<NicTO> nics = new ArrayList<NicTO>(spec.getDestNics().size());
        for (VmNicInventory nic : spec.getDestNics()) {
            nics.add(completeNicInfo(nic));
        }
        cmd.setNics(nics);

        ImageInventory image = spec.getImageSpec().getInventory();
        if (spec.getCurrentVmOperation() == VmOperation.NewCreate && ImageMediaType.ISO.toString().equals(image.getMediaType())) {
            IsoSpec iso = spec.getDestIso();

            if (iso.getInstallPath().startsWith("http")) {
                String libvirtVersion = KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(self.getUuid(), KVMSystemTags.LIBVIRT_VERSION_TOKEN);
                if ("1.2.1".compareTo(libvirtVersion) > 0) {
                    throw new OperationFailureException(errf.stringToOperationError(String.format("the ISO[%s] is exposed by HTTP protocol that needs minimal libvirt version 1.2.1, but current libvirt version is %s",
                            iso.getInstallPath(), libvirtVersion)));
                }
            }

            IsoTO bootIso = new IsoTO();
            bootIso.setPath(iso.getInstallPath());
            bootIso.setImageUuid(iso.getImageUuid());
            cmd.setBootIso(bootIso);
            cmd.setBootDev(BootDev.cdrom.toString());
        } else {
            cmd.setBootDev(BootDev.hd.toString());
        }
        
        KVMHostInventory khinv = KVMHostInventory.valueOf(getSelf());
        try {
            extEmitter.beforeStartVmOnKvm(khinv, spec, cmd);
        } catch (KVMException e) {
            String err = String.format("failed to start vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s], because %s", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                    self.getUuid(), self.getManagementIp(), e.getMessage());
            logger.warn(err, e);
            throw new OperationFailureException(errf.stringToOperationError(err));
        }

        extEmitter.addOn(khinv, spec, cmd);

        restf.asyncJsonPost(startVmPath, cmd, new JsonAsyncRESTCallback<StartVmResponse>(msg, completion) {
            @Override
            public void fail(ErrorCode err) {
                StartVmOnHypervisorReply reply = new StartVmOnHypervisorReply();
                reply.setError(err);
                reply.setSuccess(false);
                extEmitter.startVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), spec, err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(StartVmResponse ret) {
                StartVmOnHypervisorReply reply = new StartVmOnHypervisorReply();
                if (ret.isSuccess()) {
                    String info = String.format("successfully start vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                            self.getUuid(), self.getManagementIp());
                    logger.debug(info);
                    extEmitter.startVmOnKvmSuccess(KVMHostInventory.valueOf(getSelf()), spec);
                } else {
                    String err = String.format("failed to start vm[uuid:%s name:%s] on kvm host[uuid:%s, ip:%s], because %s", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                            self.getUuid(), self.getManagementIp(), ret.getError());
                    reply.setError(errf.instantiateErrorCode(HostErrors.FAILED_TO_START_VM_ON_HYPERVISOR, err));
                    logger.warn(err);
                    extEmitter.startVmOnKvmFailed(KVMHostInventory.valueOf(getSelf()), spec, reply.getError());
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<StartVmResponse> getReturnClass() {
                return StartVmResponse.class;
            }
        }, TimeUnit.SECONDS, TimeUnit.MILLISECONDS.toSeconds(msg.getTimeout()));
    }

    private void handle(final StartVmOnHypervisorMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                startVm(msg.getVmSpec(), msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("start-vm-on-kvm-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void handle(final CheckNetworkPhysicalInterfaceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                checkPhysicalInterface(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("check-network-physical-interface-on-host-%s", self.getUuid());
            }

            @Override
            protected int getSyncLevel() {
                return getHostSyncLevel();
            }
        });
    }

    private void checkPhysicalInterface(CheckNetworkPhysicalInterfaceMsg msg, NoErrorCompletion completion) {
        checkState();
        CheckPhysicalNetworkInterfaceCmd cmd = new CheckPhysicalNetworkInterfaceCmd();
        cmd.addInterfaceName(msg.getPhysicalInterface());
        CheckNetworkPhysicalInterfaceReply reply = new CheckNetworkPhysicalInterfaceReply();
        CheckPhysicalNetworkInterfaceResponse rsp = restf.syncJsonPost(checkPhysicalNetworkInterfacePath, cmd, CheckPhysicalNetworkInterfaceResponse.class);
        if (!rsp.isSuccess()) {
            String err = rsp.getFailedInterfaceNames().isEmpty() ? rsp.getError() : String.format(
                    "%s, failed to check physical network interfaces[names : %s] on kvm host[uuid:%s, ip:%s]", rsp.getError(), msg.getPhysicalInterface(), context.getInventory()
                            .getUuid(), context.getInventory().getManagementIp());
            reply.setError(errf.stringToOperationError(err));
            logger.warn(err);
        }
        bus.reply(msg, reply);
        completion.done();
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    @Override
    public void changeStateHook(HostState current, HostStateEvent stateEvent, HostState next) {
    }

    @Override
    public void deleteHook() {
    }

    @Override
    protected HostInventory getSelfInventory() {
        return KVMHostInventory.valueOf(getSelf());
    }

    protected void pingHook(final Completion completion) {
        PingCmd cmd = new PingCmd();
        restf.asyncJsonPost(pingPath, cmd, new JsonAsyncRESTCallback<PingResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(PingResponse ret) {
                if (!ret.isSuccess()) {
                    completion.fail(errf.stringToOperationError(ret.getError()));
                } else {
                    if (!self.getUuid().equals(ret.getHostUuid())) {
                        String info = String.format("detected abnormal status[host uuid change, expected: %s but: %s] of kvmagent," +
                                "it's mainly caused by kvmagent restarts behind zstack management server. Report this to ping task, it will issue a reconnect soon", self.getUuid(), ret.getHostUuid());
                        logger.warn(info);
                        completion.fail(errf.stringToOperationError(info));
                    } else {
                        completion.success();
                    }
                }
            }

            @Override
            public Class<PingResponse> getReturnClass() {
                return PingResponse.class;
            }
        });
    }

    @Override
    protected int getVmMigrateQuantity() {
        return KVMGlobalConfig.VM_MIGRATION_QUANTITY.value(Integer.class);
    }

    private ErrorCode connectToAgent() {
        ErrorCode errCode = null;
        try {
            ConnectCmd cmd = new ConnectCmd();
            cmd.setHostUuid(self.getUuid());
            ConnectResponse rsp = restf.syncJsonPost(connectPath, cmd, ConnectResponse.class);
            if (!rsp.isSuccess()) {
                String err = String.format("unable to connect to kvm host[uuid:%s, ip:%s, url:%s], because %s", self.getUuid(), self.getManagementIp(), connectPath,
                        rsp.getError());
                errCode = errf.stringToOperationError(err);
            } else {
                boolean liveSnapshot = rsp.getLibvirtVersion().compareTo(KVMConstant.MIN_LIBVIRT_LIVESNAPSHOT_VERSION) >= 0 &&
                        rsp.getQemuVersion().compareTo(KVMConstant.MIN_QEMU_LIVESNAPSHOT_VERSION) >= 0;

                String hostOS = HostSystemTags.OS_DISTRIBUTION.getTokenByResourceUuid(self.getUuid(), HostSystemTags.OS_DISTRIBUTION_TOKEN);
                liveSnapshot = liveSnapshot && (!"CentOS".equals(hostOS) || KVMGlobalConfig.ALLOW_LIVE_SNAPSHOT_ON_REDHAT.value(Boolean.class));

                if (liveSnapshot) {
                    logger.debug(String.format("kvm host[OS:%s, uuid:%s, name:%s, ip:%s] supports live snapshot with libvirt[version:%s], qemu[version:%s]",
                            hostOS, self.getUuid(), self.getName(), self.getManagementIp(), rsp.getLibvirtVersion(), rsp.getQemuVersion()));
                    HostSystemTags.LIVE_SNAPSHOT.reCreateInherentTag(self.getUuid());
                } else {
                    HostSystemTags.LIVE_SNAPSHOT.deleteInherentTag(self.getUuid());
                }
            }
        } catch (RestClientException e) {
            String err = String.format("unable to connect to kvm host[uuid:%s, ip:%s, url:%s], because %s", self.getUuid(), self.getManagementIp(),
                    connectPath, e.getMessage());
            errCode = errf.stringToOperationError(err);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            errCode = errf.throwableToInternalError(t);
        }
        
        return errCode;
    }

    private KVMHostVO getSelf() {
        return (KVMHostVO) self;
    }

    private void continueConnect(final boolean newAdded, final Completion completion) {
        ErrorCode errCode = connectToAgent();
        if (errCode != null) {
            throw new OperationFailureException(errCode);
        }

        for (KVMHostConnectExtensionPoint extp : factory.getConnectExtensions()) {
            try {
                KVMHostConnectedContext ctx = new KVMHostConnectedContext(factory.getHostContext(self.getUuid()), newAdded);
                extp.kvmHostConnected(ctx);
            } catch (Exception e) {
                String err = String.format("connection error for KVM host[uuid:%s, ip:%s] when calling %s, because %s", self.getUuid(),
                        self.getManagementIp(), extp.getClass().getName(), e.getMessage());
                logger.warn(err, e);
                throw new OperationFailureException(errf.instantiateErrorCode(HostErrors.CONNECTION_ERROR, err));
            }
        }

        completion.success();
    }

    private void createHostVersionSystemTags(String distro, String release, String version) {
        HostSystemTags.OS_DISTRIBUTION.createInherentTag(self.getUuid(), map(e(HostSystemTags.OS_DISTRIBUTION_TOKEN, distro)));
        HostSystemTags.OS_RELEASE.createInherentTag(self.getUuid(), map(e(HostSystemTags.OS_RELEASE_TOKEN, release)));
        HostSystemTags.OS_VERSION.createInherentTag(self.getUuid(), map(e(HostSystemTags.OS_VERSION_TOKEN, version)));
    }
    
    @Override
    public void connectHook(final ConnectHostInfo info, final Completion complete) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            if (info.isNewAdded()) {
                createHostVersionSystemTags("zstack", "kvmSimulator", "0.1");
                KVMSystemTags.LIBVIRT_VERSION.createInherentTag(self.getUuid(), map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.2.9")));
                KVMSystemTags.QEMU_IMG_VERSION.createInherentTag(self.getUuid(), map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.0.0")));
            }

            continueConnect(info.isNewAdded(), complete);
        } else {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("run-ansible-for-kvm-%s", self.getUuid()));
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    if (info.isNewAdded()) {
                        flow(new NoRollbackFlow() {
                            String __name__ = "ping-DNS-check-list";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                String checkList = KVMGlobalConfig.HOST_DNS_CHECK_LIST.value();
                                checkList = checkList.replaceAll(",", " ");
                                SshResult ret = new Ssh().setHostname(getSelf().getManagementIp())
                                        .setUsername(getSelf().getUsername()).setPassword(getSelf().getPassword())
                                        .script("scripts/check-public-dns-name.sh", map(e("dnsCheckList", checkList))).runAndClose();
                                if (ret.isSshFailure()) {
                                    trigger.fail(errf.stringToOperationError(
                                            String.format("unable to connect to KVM[ip:%s, username:%s] to do DNS check, please check if username/password is wrong; %s", self.getManagementIp(), getSelf().getUsername(), ret.getExitErrorMessage())
                                    ));
                                } else if (ret.getReturnCode() != 0) {
                                    trigger.fail(errf.stringToOperationError(
                                            String.format("failed to ping all DNS/IP in %s; please check /etc/resolv.conf to make sure your host is able to reach public internet, or change host.DNSCheckList if you have some special network setup",
                                                    KVMGlobalConfig.HOST_DNS_CHECK_LIST.value())
                                    ));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    flow(new NoRollbackFlow() {
                        String __name__ = "apply-ansible-playbook";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            String srcPath = PathUtil.findFileOnClassPath(String.format("ansible/kvm/%s", agentPackageName), true).getAbsolutePath();
                            String destPath = String.format("/var/lib/zstack/kvm/%s", agentPackageName);
                            SshFileMd5Checker checker = new SshFileMd5Checker();
                            checker.setUsername(getSelf().getUsername());
                            checker.setPassword(getSelf().getPassword());
                            checker.setTargetIp(getSelf().getManagementIp());
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/kvm/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(srcPath, destPath);

                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.setAgentPort(KVMGlobalProperty.AGENT_PORT);
                            runner.setTargetIp(getSelf().getManagementIp());
                            runner.setPlayBookName(KVMConstant.ANSIBLE_PLAYBOOK_NAME);
                            runner.setUsername(getSelf().getUsername());
                            runner.setPassword(getSelf().getPassword());
                            if (info.isNewAdded()) {
                                runner.putArgument("init", "true");
                                runner.setFullDeploy(true);
                            }
                            runner.putArgument("pkg_kvmagent", agentPackageName);
                            runner.putArgument("hostname", String.format("%s.zstack.org",self.getManagementIp().replaceAll("\\.", "-")));
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
                        String __name__ = "echo-host";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            restf.echo(echoPath, new Completion(trigger) {
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

                    if (info.isNewAdded()) {
                        flow(new NoRollbackFlow() {
                            String __name__ = String.format("ansbile-get-kvm-host-facts");

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                String privKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PRIVATE_KEY).getAbsolutePath();
                                ShellResult ret = ShellUtils.runAndReturn(String.format("ansible -i %s --private-key %s -m setup %s",
                                        AnsibleConstant.INVENTORY_FILE, privKeyFile, self.getManagementIp()), AnsibleConstant.ROOT_DIR);
                                if (!ret.isReturnCode(0)) {
                                    trigger.fail(errf.stringToOperationError(
                                            String.format("unable to get kvm host[uuid:%s, ip:%s] facts by ansible\n%s", self.getUuid(), self.getManagementIp(), ret.getExecutionLog())
                                    ));

                                    return;
                                }

                                String[] pairs = ret.getStdout().split(">>");
                                if (pairs.length != 2) {
                                    trigger.fail(errf.stringToOperationError(String.format("unrecognized ansible facts mediaType, %s", ret.getStdout())));
                                    return;
                                }

                                LinkedHashMap output = JSONObjectUtil.toObject(pairs[1], LinkedHashMap.class);
                                LinkedHashMap facts = (LinkedHashMap) output.get("ansible_facts");
                                if (facts == null) {
                                    trigger.fail(errf.stringToOperationError(String.format("unrecognized ansible facts mediaType, cannot find field 'ansible_facts', %s", ret.getStdout())));
                                    return;
                                }

                                String distro = (String) facts.get("ansible_distribution");
                                String release = (String) facts.get("ansible_distribution_release");
                                String version = (String) facts.get("ansible_distribution_version");
                                createHostVersionSystemTags(distro, release, version);
                                trigger.next();
                            }
                        });
                    }

                    flow(new NoRollbackFlow() {
                        String __name__ = "collect-kvm-host-facts";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            HostFactCmd cmd = new HostFactCmd();
                            restf.asyncJsonPost(hostFactPath, cmd, new JsonAsyncRESTCallback<HostFactResponse>() {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(HostFactResponse ret) {
                                    if (!ret.isSuccess()) {
                                        trigger.fail(errf.stringToOperationError(ret.getError()));
                                        return;
                                    }

                                    if (ret.getHvmCpuFlag() == null) {
                                        trigger.fail(errf.stringToOperationError(
                                                "cannot find either 'vmx' or 'svm' in /proc/cpuinfo, please make sure you have enabled virtualization in your BIOS setting"
                                        ));
                                        return;
                                    }

                                    KVMSystemTags.QEMU_IMG_VERSION.recreateTag(self.getUuid(), map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, ret.getQemuImgVersion())));
                                    KVMSystemTags.LIBVIRT_VERSION.recreateTag(self.getUuid(), map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, ret.getLibvirtVersion())));
                                    KVMSystemTags.HVM_CPU_FLAG.recreateTag(self.getUuid(), map(e(KVMSystemTags.HVM_CPU_FLAG_TOKEN, ret.getHvmCpuFlag())));

                                    if (ret.getLibvirtVersion().compareTo(KVMConstant.MIN_LIBVIRT_VIRTIO_SCSI_VERSION) >= 0) {
                                        KVMSystemTags.VIRTIO_SCSI.reCreateInherentTag(self.getUuid());
                                    }

                                    trigger.next();
                                }

                                @Override
                                public Class<HostFactResponse> getReturnClass() {
                                    return HostFactResponse.class;
                                }
                            });
                        }
                    });

                    error(new FlowErrorHandler(complete) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            complete.fail(errCode);
                        }
                    });

                    done(new FlowDoneHandler(complete) {
                        @Override
                        public void handle(Map data) {
                            continueConnect(info.isNewAdded(), complete);
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    protected int getHostSyncLevel() {
        return KVMGlobalConfig.HOST_SYNC_LEVEL.value(Integer.class);
    }

    @Override
    public void executeHostMessageHandlerHook(HostMessageHandlerExtensionPoint ext, Message msg) {
    }

    @Override
    protected HostVO updateHost(APIUpdateHostMsg msg) {
        KVMHostVO vo = (KVMHostVO) super.updateHost(msg);
        vo = vo == null ? getSelf() : vo;

        APIUpdateKVMHostMsg umsg = (APIUpdateKVMHostMsg) msg;
        if (umsg.getUsername() != null) {
            vo.setUsername(umsg.getUsername());
        }
        if (umsg.getPassword() != null) {
            vo.setPassword(umsg.getPassword());
        }

        return vo;
    }
}
