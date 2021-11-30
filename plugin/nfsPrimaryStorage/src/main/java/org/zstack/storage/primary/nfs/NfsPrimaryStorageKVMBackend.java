package org.zstack.storage.primary.nfs;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.step.StepRun;
import org.zstack.core.step.StepRunCondition;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.trash.StorageTrash;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.storage.primary.PrimaryStorageBase.PhysicalCapacityUsage;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.*;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.lang.Integer.min;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.Platform.touterr;

public class NfsPrimaryStorageKVMBackend implements NfsPrimaryStorageBackend,
        KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint,
        KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorageKVMBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageFactory nfsFactory;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private EventFacade eventf;
    @Autowired
    private StorageTrash trash;
    @Autowired
    protected ApiTimeoutManager timeoutManager;

    public static final String MOUNT_PRIMARY_STORAGE_PATH = "/nfsprimarystorage/mount";
    public static final String UNMOUNT_PRIMARY_STORAGE_PATH = "/nfsprimarystorage/unmount";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/nfsprimarystorage/createemptyvolume";
    public static final String CREATE_FOLDER_PATH = "/nfsprimarystorage/createfolder";
    public static final String GET_CAPACITY_PATH = "/nfsprimarystorage/getcapacity";
    public static final String DELETE_PATH = "/nfsprimarystorage/delete";
    public static final String CHECK_BITS_PATH = "/nfsprimarystorage/checkbits";
    public static final String MOVE_BITS_PATH = "/nfsprimarystorage/movebits";
    public static final String MERGE_SNAPSHOT_PATH = "/nfsprimarystorage/mergesnapshot";
    public static final String REBASE_MERGE_SNAPSHOT_PATH = "/nfsprimarystorage/rebaseandmergesnapshot";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/nfsprimarystorage/revertvolumefromsnapshot";
    public static final String REINIT_IMAGE_PATH = "/nfsprimarystorage/reinitimage";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/nfsprimarystorage/sftp/createtemplatefromvolume";
    public static final String CREATE_VOLUME_WITH_BACKING_PATH = "/nfsprimarystorage/createvolumewithbacking";
    public static final String OFFLINE_SNAPSHOT_MERGE = "/nfsprimarystorage/offlinesnapshotmerge";
    public static final String REMOUNT_PATH = "/nfsprimarystorage/remount";
    public static final String GET_VOLUME_SIZE_PATH = "/nfsprimarystorage/getvolumesize";
    public static final String HARD_LINK_VOLUME = "/nfsprimarystorage/volume/hardlink";
    public static final String PING_PATH = "/nfsprimarystorage/ping";
    public static final String GET_VOLUME_BASE_IMAGE_PATH = "/nfsprimarystorage/getvolumebaseimage";
    public static final String UPDATE_MOUNT_POINT_PATH = "/nfsprimarystorage/updatemountpoint";
    public static final String NFS_TO_NFS_MIGRATE_BITS_PATH = "/nfsprimarystorage/migratebits";
    public static final String NFS_REBASE_VOLUME_BACKING_FILE_PATH = "/nfsprimarystorage/rebasevolumebackingfile";
    public static final String DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/nfsprimarystorage/kvmhost/download";
    public static final String CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/nfsprimarystorage/kvmhost/download/cancel";
    public static final String GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH = "/nfsprimarystorage/kvmhost/download/progress";
    public static final String CREATE_VOLUME_FROM_TEMPLATE_PATH = "/nfsprimarystorage/sftp/createvolumefromtemplate";
    public static final String GET_QCOW2_HASH_VALUE_PATH = "/nfsprimarystorage/getqcow2hash";


    //////////////// For unit test //////////////////////////
    private boolean syncGetCapacity = false;
    public static final String SYNC_GET_CAPACITY_PATH = "/nfsprimarystorage/syncgetcapacity";
    //////////////// End for unit test //////////////////////

    private static final String QCOW3_QEMU_IMG_VERSION = "2.0.0";

    private void mount(PrimaryStorageInventory inv, String hostUuid, Completion completion) {
        MountCmd cmd = new MountCmd();
        cmd.setUrl(inv.getUrl());
        cmd.setMountPath(inv.getMountPath());
        cmd.setOptions(NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(inv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN));

        syncHttpCall(MOUNT_PRIMARY_STORAGE_PATH, hostUuid, cmd, true, MountAgentResponse.class, inv, new ReturnValueCompletion<MountAgentResponse>(completion) {
            @Override
            public void success(MountAgentResponse returnValue) {
                logger.debug(String.format(
                        "Successfully mounted nfs primary storage[uuid:%s] on kvm host[uuid:%s]",
                        inv.getUuid(), hostUuid));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (errorCode.getDetails().contains("java.net.SocketTimeoutException: Read timed out")) {
                    // socket read timeout is caused by timeout of mounting a wrong URL
                    errorCode = touterr(errorCode, "mount timeout. Please the check if the URL[%s] is" +
                            " valid to mount", inv.getUrl());
                }
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void attachToCluster(PrimaryStorageInventory inv, String clusterUuid, ReturnValueCompletion<Boolean> completion){
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            checkQemuImgVersionInOtherClusters(inv, clusterUuid);
        }

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.select(HostVO_.uuid);
        query.add(HostVO_.state, Op.NOT_IN, HostState.PreMaintenance, HostState.Maintenance);
        query.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        query.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        List<String> hostUuids = query.listValue();
        if(hostUuids.isEmpty()){
            completion.success(false);// return !isEmpty
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(hostUuids).all((hostUuid, compl) -> {
            mount(inv, hostUuid, new Completion(compl){

                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errs.add(errorCode);
                    compl.done();
                }
            });

        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if(!errs.isEmpty()){
                    completion.fail(errs.get(0));
                }else {
                    completion.success(true);
                }
            }
        });
    }

    private void checkQemuImgVersionInOtherClusters(final PrimaryStorageInventory inv, String clusterUuid) {
        SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
        hq.select(HostVO_.uuid);
        hq.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        List<String> huuidsInCluster = hq.listValue();
        if (huuidsInCluster.isEmpty()) {
            return;
        }

        Map<String, List<String>> qtags = KVMSystemTags.QEMU_IMG_VERSION.getTags(huuidsInCluster);
        if (qtags.isEmpty()) {
            // the hosts may be still in Connecting
            return;
        }

        List<String> huuidsAttachedPrimaryStorage = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select h.uuid from HostVO h, PrimaryStorageClusterRefVO ref where h.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid = :psUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuid", inv.getUuid());
                return q.getResultList();
            }
        }.call();

        if (huuidsAttachedPrimaryStorage.isEmpty()) {
            return;
        }

        String versionInCluster = KVMSystemTags.QEMU_IMG_VERSION.getTokenByTag(qtags.values().iterator().next().get(0), KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
        qtags = KVMSystemTags.QEMU_IMG_VERSION.getTags(huuidsAttachedPrimaryStorage);
        for (Entry<String, List<String>> e : qtags.entrySet()) {
            String otherVersion = KVMSystemTags.QEMU_IMG_VERSION.getTokenByTag(e.getValue().get(0), KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
            if ((versionInCluster.compareTo(QCOW3_QEMU_IMG_VERSION) >= 0 && otherVersion.compareTo(QCOW3_QEMU_IMG_VERSION) < 0) ||
                    (versionInCluster.compareTo(QCOW3_QEMU_IMG_VERSION) < 0 && otherVersion.compareTo(QCOW3_QEMU_IMG_VERSION) >= 0)) {
                ErrorCode err = operr(
                        "unable to attach a primary storage[uuid:%s, name:%s] to cluster[uuid:%s]. Kvm host in the cluster has qemu-img "
                                + "with version[%s]; but the primary storage has attached to another cluster that has kvm host which has qemu-img with "
                                + "version[%s]. qemu-img version greater than %s is incompatible with versions less than %s, this will causes volume snapshot operation "
                                + "to fail. Please avoid attaching a primary storage to clusters that have different Linux distributions, in order to prevent qemu-img version mismatch",
                        inv.getUuid(), inv.getName(), clusterUuid, versionInCluster, otherVersion, QCOW3_QEMU_IMG_VERSION, QCOW3_QEMU_IMG_VERSION
                );

                throw new OperationFailureException(err);
            }
        }
    }

    private void unmount(PrimaryStorageInventory inv, String hostUuid) {
        UnmountCmd cmd = new UnmountCmd();
        cmd.setUuid(inv.getUuid());
        cmd.setMountPath(inv.getMountPath());
        cmd.setUrl(inv.getUrl());

        KVMHostSyncHttpCallMsg msg = new KVMHostSyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(UNMOUNT_PRIMARY_STORAGE_PATH);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        AgentResponse rsp = ((KVMHostSyncHttpCallReply) reply).toResponse(AgentResponse.class);
        if (!rsp.isSuccess()) {
            String err = String.format("Unable to unmount nfs primary storage[uuid:%s] on kvm host[uuid:%s], because %s", inv.getUuid(), hostUuid, rsp.getError());
            logger.warn(err);
        } else {
            String info = String.format("Successfully unmount nfs primary storage[uuid:%s] on kvm host[uuid:%s]", inv.getUuid(), hostUuid);
            logger.debug(info);
            nfsFactory.updateNfsHostStatus(inv.getUuid(), hostUuid, PrimaryStorageHostStatus.Disconnected);
        }
    }

    @Override
    public void detachFromCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException {
        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.select(HostVO_.uuid);
        query.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        query.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        List<String> hostUuids = query.listValue();
        for (String huuid : hostUuids) {
            unmount(inv, huuid);
        }
    }

    @Override
    public void createMemoryVolume(PrimaryStorageInventory pinv, VolumeInventory volume, ReturnValueCompletion<String> completion) {
        final CreateFolderCmd cmd = new CreateFolderCmd();
        cmd.setUuid(pinv.getUuid());
        cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeVolumeInstallDir(pinv, volume));

        final HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(CREATE_FOLDER_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                NfsPrimaryStorageAgentResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(NfsPrimaryStorageAgentResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("unable to create folder[installUrl:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            cmd.getInstallUrl(), host.getUuid(), host.getManagementIp(), rsp.getError());
                    completion.fail(err);
                    return;
                }

                nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                completion.success(cmd.getInstallUrl());
            }
        });

    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    private void sendWarnning(String hostUuid, PrimaryStorageInventory ps) {
        HostCanonicalEvents.HostMountData data = new HostCanonicalEvents.HostMountData();
        data.hostUuid = hostUuid;
        data.psUuid = ps.getUuid();
        data.details = String.format("host: [%s] not mount url [%s] on mountpath [%s]", hostUuid, ps.getUrl(), ps.getMountPath());
        eventf.fire(HostCanonicalEvents.HOST_CHECK_MOUNT_FAULT, data);
    }

    @Override
    public void ping(PrimaryStorageInventory inv, final Completion completion) {
        final int ALL_LIMIT = 3;
        List<HostInventory> hosts;
        try {
            hosts = nfsFactory.getConnectedHostForPing(inv);
        }catch (OperationFailureException e){
            pingAll(inv, ALL_LIMIT, new Completion(completion) {
                @Override
                public void success() {
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        new StepRun<HostInventory>(hosts) {
            @Override
            @StepRunCondition(stepLimit = 100)
            protected void call(List<HostInventory> stepHosts, Completion completion) {
                pingFilterStep(stepHosts, inv, completion);
            }
        }.run(completion);
    }

    private void pingFilterStep(List<HostInventory> hosts, PrimaryStorageInventory inv, final Completion completion){
        doPing(hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList()), inv, new Completion(completion) {
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

    private void pingAll(PrimaryStorageInventory inv, int limit, final Completion completion){
        List<String> huuids = Q.New(HostVO.class).select(HostVO_.uuid)
                                    .in(HostVO_.clusterUuid, inv.getAttachedClusterUuids())
                                    .eq(HostVO_.status, HostStatus.Connected)
                                    .eq(HostVO_.state, HostState.Enabled)
                                    .listValues();

        if(huuids.size() == 0){
            completion.fail(operr("no host in is Connected or primary storage[uuid:%s] attach no cluster", inv.getUuid()));
            return;
        }

        Collections.shuffle(huuids);
        doPing(huuids.subList(0, min(limit,huuids.size())), inv, new Completion(completion) {
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

    private void doPing(List<String> hostUuids, PrimaryStorageInventory psInv, Completion completion){
        List<ErrorCode> errs = new ArrayList<>();
        new While<>(hostUuids).each((huuid, compl) -> {
            PingCmd cmd = new PingCmd();
            cmd.setUuid(psInv.getUuid());
            cmd.mountPath = psInv.getMountPath();
            cmd.url = psInv.getUrl();

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setPath(PING_PATH);
            msg.setHostUuid(huuid);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        NfsPrimaryStorageAgentResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(NfsPrimaryStorageAgentResponse.class);
                        if (rsp.isSuccess()) {
                            nfsFactory.updateNfsHostStatus(psInv.getUuid(), huuid, PrimaryStorageHostStatus.Connected);
                        } else {
                            ErrorCode err = operr("failed to ping nfs primary storage[uuid:%s] from host[uuid:%s],because %s. " +
                                            "disconnect this host-ps connection",
                                    psInv.getUuid(), huuid, reply.isSuccess() ? rsp.getError() : reply.getError());
                            errs.add(err);
                            nfsFactory.updateNfsHostStatus(psInv.getUuid(), huuid, PrimaryStorageHostStatus.Disconnected,
                                    () -> sendWarnning(huuid, psInv));
                        }
                    } else {
                        errs.add(reply.getError());
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() == hostUuids.size()) {
                    completion.fail(errs.get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, CreateTemporaryVolumeFromSnapshotMsg msg, final ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        VolumeSnapshotInventory sp = msg.getSnapshot();
        final String workspaceInstallPath = NfsPrimaryStorageKvmHelper.makeSnapshotWorkspacePath(inv, msg.getTemporaryVolumeUuid());

        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());
        cmd.setWorkspaceInstallPath(workspaceInstallPath);
        cmd.setUuid(inv.getUuid());
        cmd.setVolumeUuid(sp.getVolumeUuid());

        new KvmCommandSender(host.getUuid()).send(cmd, MERGE_SNAPSHOT_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setInstallPath(workspaceInstallPath);
                reply.setActualSize(rsp.getActualSize());
                reply.setSize(rsp.getSize());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);

        GetKVMHostDownloadCredentialMsg gmsg = new GetKVMHostDownloadCredentialMsg();
        gmsg.setHostUuid(msg.getSrcHostUuid());

        if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.hasTag(inv.getUuid())) {
            gmsg.setDataNetworkCidr(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByResourceUuid(inv.getUuid(), PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN));
        }

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, msg.getSrcHostUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    completion.fail(rly.getError());
                    return;
                }

                GetKVMHostDownloadCredentialReply grly = rly.castReply();
                DownloadBitsFromKVMHostCmd cmd = new DownloadBitsFromKVMHostCmd();
                cmd.hostname = grly.getHostname();
                cmd.username = grly.getUsername();
                cmd.sshKey = grly.getSshKey();
                cmd.sshPort = grly.getSshPort();
                cmd.backupStorageInstallPath = msg.getHostInstallPath();
                cmd.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();
                cmd.bandWidth = msg.getBandWidth();
                cmd.identificationCode = msg.getLongJobUuid() + msg.getPrimaryStorageInstallPath();

                new KvmCommandSender(host.getUuid()).send(cmd, DOWNLOAD_BITS_FROM_KVM_HOST_PATH, new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        DownloadBitsFromKVMHostRsp rsp = wrapper.getResponse(DownloadBitsFromKVMHostRsp.class);
                        return rsp.isSuccess() ? null : operr("%s", rsp.getError());
                    }
                }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper wrapper) {
                        DownloadBitsFromKVMHostRsp rsp = wrapper.getResponse(DownloadBitsFromKVMHostRsp.class);
                        DownloadBitsFromKVMHostToPrimaryStorageReply rly = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                        rly.setFormat(rsp.format);
                        completion.success(rly);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);

        CancelDownloadBitsFromKVMHostCmd cmd = new CancelDownloadBitsFromKVMHostCmd();
        cmd.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();

        new KvmCommandSender(host.getUuid()).send(cmd, CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                completion.success(new CancelDownloadBitsFromKVMHostToPrimaryStorageReply());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);

        GetDownloadBitsFromKVMHostProgressCmd cmd = new GetDownloadBitsFromKVMHostProgressCmd();
        cmd.volumePaths = msg.getVolumePaths();

        new KvmCommandSender(host.getUuid()).send(cmd, GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetDownloadBitsFromKVMHostProgressRsp rsp = wrapper.getResponse(GetDownloadBitsFromKVMHostProgressRsp.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                GetDownloadBitsFromKVMHostProgressRsp rsp = wrapper.getResponse(GetDownloadBitsFromKVMHostProgressRsp.class);
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setTotalSize(rsp.totalSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        VolumeSnapshotInventory sp = msg.getSnapshot();

        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            createIncrementalVolumeFromSnapshot(sp, msg.getVolumeUuid(), inv, host, completion);
        } else {
            createNormalVolumeFromSnapshot(sp, msg.getVolumeUuid(), inv, host, completion);
        }
    }

    private void createNormalVolumeFromSnapshot(VolumeSnapshotInventory sp, String volumeUuid, PrimaryStorageInventory inv, HostInventory host, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String volPath = NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(inv, volumeUuid);

        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());
        cmd.setWorkspaceInstallPath(volPath);
        cmd.setUuid(inv.getUuid());
        cmd.setVolumeUuid(sp.getVolumeUuid());

        new KvmCommandSender(host.getUuid()).send(cmd, MERGE_SNAPSHOT_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                reply.setActualSize(rsp.getActualSize());
                reply.setSize(rsp.getSize());
                reply.setInstallPath(volPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createIncrementalVolumeFromSnapshot(VolumeSnapshotInventory sp, String volumeUuid, PrimaryStorageInventory inv, HostInventory host, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        final String volPath = NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(inv, volumeUuid);

        String accounUuid = acntMgr.getOwnerAccountUuidOfResource(volumeUuid);

        CreateVolumeWithBackingCmd cmd = new CreateVolumeWithBackingCmd();
        cmd.setTemplatePathInCache(sp.getPrimaryStorageInstallPath());
        cmd.setInstallUrl(volPath);

        cmd.setUuid(inv.getUuid());
        cmd.setVolumeUuid(sp.getVolumeUuid());
        cmd.setAccountUuid(accounUuid);
        cmd.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);

        new KvmCommandSender(host.getUuid()).send(cmd, CREATE_VOLUME_WITH_BACKING_PATH, wrapper -> {
            CreateVolumeWithBackingRsp rsp = wrapper.getResponse(CreateVolumeWithBackingRsp.class);
            return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                CreateVolumeWithBackingRsp rsp = wrapper.getResponse(CreateVolumeWithBackingRsp.class);
                reply.setActualSize(rsp.getActualSize());
                reply.setSize(rsp.getSize());
                reply.setInstallPath(volPath);
                createProtectTag();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }

            private void createProtectTag() {
                SystemTagCreator creator = VolumeSnapshotSystemTags.BACKING_TO_VOLUME.newSystemTagCreator(sp.getUuid());
                creator.unique = false;
                creator.setTagByTokens(Collections.singletonMap(VolumeSnapshotSystemTags.BACKING_VOLUME_TOKEN, volumeUuid));
                creator.create();
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, UploadBitsToBackupStorageMsg msg, final ReturnValueCompletion<UploadBitsToBackupStorageReply> completion) {
        BackupStorageVO bs = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);

        NfsPrimaryToBackupStorageMediator m = nfsFactory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()), HypervisorType.valueOf(msg.getHypervisorType()));
        m.uploadBits(msg.getImageUuid(), inv, BackupStorageInventory.valueOf(bs), msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String installPath) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setBackupStorageInstallPath(installPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, SyncVolumeSizeOnPrimaryStorageMsg msg, final ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion) {
        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        KvmCommandSender sender = new KvmCommandSender(host.getUuid());

        GetVolumeActualSizeCmd cmd = new GetVolumeActualSizeCmd();
        cmd.setUuid(inv.getUuid());
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();
        sender.send(cmd, GET_VOLUME_SIZE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeActualSizeRsp rsp = wrapper.getResponse(GetVolumeActualSizeRsp.class);
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                GetVolumeActualSizeRsp rsp = returnValue.getResponse(GetVolumeActualSizeRsp.class);
                reply.setSize(rsp.size);
                reply.setActualSize(rsp.actualSize);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, GetVolumeRootImageUuidFromPrimaryStorageMsg msg, final ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion) {
        GetVolumeBaseImagePathCmd cmd = new GetVolumeBaseImagePathCmd();
        cmd.volumeUuid = msg.getVolume().getUuid();
        cmd.volumeInstallDir = NfsPrimaryStorageKvmHelper.makeVolumeInstallDir(inv, msg.getVolume());
        cmd.imageCacheDir = NfsPrimaryStorageKvmHelper.getCachedImageDir(inv);

        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        new KvmCommandSender(host.getUuid()).send(cmd, GET_VOLUME_BASE_IMAGE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeBaseImagePathRsp rsp = wrapper.getResponse(GetVolumeBaseImagePathRsp.class);
                if (rsp.isSuccess() && StringUtils.isEmpty(rsp.path)) {
                    return operr("cannot get root image of volume[uuid:%s], may be it create from iso", msg.getVolume().getUuid());
                }
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                GetVolumeBaseImagePathRsp rsp = w.getResponse(GetVolumeBaseImagePathRsp.class);
                File f = new File(rsp.path);
                String rootImageUuid = f.getName().split("\\.")[0];
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setImageUuid(rootImageUuid);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory dstPsInv, NfsToNfsMigrateBitsMsg msg, ReturnValueCompletion<NfsToNfsMigrateBitsReply> completion) {
        HostVO hostVO = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
        if (hostVO == null) {
            throw new OperationFailureException(operr("The chosen host[uuid:%s] to perform storage migration is lost", msg.getHostUuid()));
        }
        HostInventory host = HostInventory.valueOf(hostVO);

        // check if need to mount ps to host first
        boolean mounted = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, host.getClusterUuid())
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, dstPsInv.getUuid())
                .isExists();

        if (logger.isTraceEnabled()) {
            if (mounted) {
                logger.info(String.format("no need to mount nfs ps[uuid:%s] to host[uuid:%s]", dstPsInv.getUuid(), host.getUuid()));
            }
        }

        NfsToNfsMigrateBitsCmd cmd = new NfsToNfsMigrateBitsCmd();
        cmd.srcFolderPath = msg.getSrcFolderPath();
        cmd.dstFolderPath = msg.getDstFolderPath();
        cmd.filtPaths = trash.findTrashInstallPath(msg.getSrcFolderPath(), msg.getSrcPrimaryStorageUuid());
        cmd.isMounted = mounted;

        if (!mounted) {
            cmd.options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(dstPsInv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);
            cmd.url = dstPsInv.getUrl();
            cmd.mountPath = dstPsInv.getMountPath();
        }

        new KvmCommandSender(host.getUuid()).send(cmd, NFS_TO_NFS_MIGRATE_BITS_PATH, wrapper -> {
            NfsToNfsMigrateBitsRsp rsp = wrapper.getResponse(NfsToNfsMigrateBitsRsp.class);
            return rsp.isSuccess() ? null : operr("%s", rsp.getError());
        }, msg.getTimeout(), new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                logger.info("successfully copyed volume folder to nfs ps " + dstPsInv.getUuid());
                NfsToNfsMigrateBitsReply reply = new NfsToNfsMigrateBitsReply();
                completion.success(reply);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                logger.error("failed to copy volume folder to nfs ps " + dstPsInv.getUuid());
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, NfsRebaseVolumeBackingFileMsg msg, ReturnValueCompletion<NfsRebaseVolumeBackingFileReply> completion) {
        NfsRebaseVolumeBackingFileCmd cmd = new NfsRebaseVolumeBackingFileCmd();
        cmd.srcPsMountPath = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.mountPath).eq(PrimaryStorageVO_.uuid, msg.getSrcPsUuid()).findValue();
        cmd.dstPsMountPath = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.mountPath).eq(PrimaryStorageVO_.uuid, msg.getDstPsUuid()).findValue();
        cmd.dstVolumeFolderPath = msg.getDstVolumeFolderPath();
        cmd.dstImageCacheTemplateFolderPath = msg.getDstImageCacheTemplateFolderPath();

        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        new KvmCommandSender(host.getUuid()).send(cmd, NFS_REBASE_VOLUME_BACKING_FILE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                NfsRebaseVolumeBackingFileRsp rsp = wrapper.getResponse(NfsRebaseVolumeBackingFileRsp.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                logger.info("successfully rebased backing file for qcow2 files in " + msg.getDstVolumeFolderPath());
                NfsRebaseVolumeBackingFileReply reply = new NfsRebaseVolumeBackingFileReply();
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error("failed to rebase backing file for qcow2 files in " + msg.getDstVolumeFolderPath());
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void getPhysicalCapacity(PrimaryStorageInventory inv, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);

        GetCapacityCmd cmd = new GetCapacityCmd();
        cmd.setMountPath(inv.getMountPath());
        cmd.setUuid(inv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(host.getUuid());
        msg.setPath(GET_CAPACITY_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                GetCapacityResponse rsp = r.toResponse(GetCapacityResponse.class);
                if (!r.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
                usage.totalPhysicalSize = rsp.getTotalCapacity();
                usage.availablePhysicalSize = rsp.getAvailableCapacity();
                completion.success(usage);
            }
        });
    }

    @Override
    public void checkIsBitsExisting(final PrimaryStorageInventory inv, final String installPath, final ReturnValueCompletion<Boolean> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv).get(0);
        CheckIsBitsExistingCmd cmd = new CheckIsBitsExistingCmd();
        cmd.setUuid(inv.getUuid());
        cmd.setHostUuid(host.getUuid());
        cmd.setInstallPath(installPath);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(CHECK_BITS_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CheckIsBitsExistingRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(CheckIsBitsExistingRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to check existence of %s on nfs primary storage[uuid:%s], %s",
                                    installPath, inv.getUuid(), rsp.getError()));
                    return;
                }

                nfsMgr.reportCapacityIfNeeded(inv.getUuid(), rsp);
                completion.success(rsp.isExisting());
            }
        });
    }

    @Transactional(readOnly = true)
    private List<PrimaryStorageInventory> getPrimaryStorageForHost(String clusterUuid) {
        String sql = "select p.uuid, p.url, p.mountPath from PrimaryStorageVO p where p.type = :ptype and p.uuid in (select r.primaryStorageUuid from PrimaryStorageClusterRefVO r where r.clusterUuid = :clusterUuid)";
        Query query = dbf.getEntityManager().createQuery(sql);
        query.setParameter("clusterUuid", clusterUuid);
        query.setParameter("ptype", NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        List<Object[]> lst = query.getResultList();
        List<PrimaryStorageInventory> pss = new ArrayList<PrimaryStorageInventory>();
        for (Object[] objs : lst) {
            PrimaryStorageInventory inv = new PrimaryStorageInventory();
            inv.setUuid((String) objs[0]);
            inv.setUrl((String) objs[1]);
            inv.setMountPath((String) objs[2]);
            pss.add(inv);
        }
        return pss;
    }


    private void checkQemuImgVersionInOtherClusters(KVMHostConnectedContext context, List<PrimaryStorageInventory> invs) {
        String mine = KVMSystemTags.QEMU_IMG_VERSION.getTokenByResourceUuid(context.getInventory().getUuid(), KVMSystemTags.QEMU_IMG_VERSION_TOKEN);

        final List<String> psUuids = CollectionUtils.transformToList(invs, new Function<String, PrimaryStorageInventory>() {
            @Override
            public String call(PrimaryStorageInventory arg) {
                return arg.getUuid();
            }
        });

        List<String> otherHostUuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select host.uuid from HostVO host, PrimaryStorageClusterRefVO ref where host.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid in (:psUuids)";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuids", psUuids);
                return q.getResultList();
            }
        }.call();

        Map<String, List<String>> qemuTags = KVMSystemTags.QEMU_IMG_VERSION.getTags(otherHostUuids);
        for (Entry<String, List<String>> e : qemuTags.entrySet()) {
            String version = KVMSystemTags.QEMU_IMG_VERSION.getTokenByTag(e.getValue().get(0), KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
            if (
                    (version.compareTo(QCOW3_QEMU_IMG_VERSION) >= 0 && mine.compareTo(QCOW3_QEMU_IMG_VERSION) < 0) ||
                            (version.compareTo(QCOW3_QEMU_IMG_VERSION) < 0 && mine.compareTo(QCOW3_QEMU_IMG_VERSION) >= 0)
                    ) {
                ErrorCode err = operr(
                        "unable to attach a primary storage to cluster. Kvm host[uuid:%s, name:%s] in cluster has qemu-img "
                                + "with version[%s]; but the primary storage has attached to a cluster that has kvm host[uuid:%s], which has qemu-img with "
                                + "version[%s]. qemu-img version greater than %s is incompatible with versions less than %s, this will causes volume snapshot operation "
                                + "to fail. Please avoid attaching a primary storage to clusters that have different Linux distributions, in order to prevent qemu-img version mismatch",
                        context.getInventory().getUuid(), context.getInventory().getName(), mine, e.getKey(), version, QCOW3_QEMU_IMG_VERSION, QCOW3_QEMU_IMG_VERSION
                );
                throw new OperationFailureException(err);
            }
        }
    }

    @Override
    public void instantiateVolume(final PrimaryStorageInventory pinv, final VolumeInventory volume, final ReturnValueCompletion<VolumeInventory> complete) {
        String accounUuid = acntMgr.getOwnerAccountUuidOfResource(volume.getUuid());

        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.setUuid(pinv.getUuid());
        cmd.setAccountUuid(accounUuid);
        cmd.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
        cmd.setName(volume.getName());
        cmd.setSize(volume.getSize());
        cmd.setVolumeUuid(volume.getUuid());
        if (StringUtils.isNotEmpty(volume.getInstallPath())) {
            cmd.setInstallUrl(volume.getInstallPath());
        } else if (volume.getType().equals(VolumeType.Root.toString())) {
            cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeRootVolumeInstallUrl(pinv, volume));
        } else if (volume.getType().equals(VolumeType.Data.toString())) {
            cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(pinv, volume.getUuid()));
        } else if (volume.getType().equals(VolumeType.Cache.toString())) {
            cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(pinv, volume.getUuid()));
        } else {
            throw new CloudRuntimeException(String.format("unknown volume type %s", volume.getType()));
        }

        if (volume.getType().equals(VolumeType.Memory.toString())) {
            cmd.setWithoutVolume(true);
        }

        final HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(CREATE_EMPTY_VOLUME_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                CreateEmptyVolumeResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(CreateEmptyVolumeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("unable to create empty volume[uuid:%s,  name:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            volume.getUuid(), volume.getName(), host.getUuid(), host.getManagementIp(), rsp.getError());
                    complete.fail(err);
                    return;
                }

                volume.setInstallPath(cmd.getInstallUrl());
                volume.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);

                nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                complete.success(volume);
            }
        });
    }

    public void setSyncGetCapacity(boolean syncGetCapacity) {
        this.syncGetCapacity = syncGetCapacity;
    }

    @Transactional
    private List<PrimaryStorageVO> getPrimaryStorageHostShouldMount(HostInventory host) {
        String sql = "select p from PrimaryStorageVO p where p.type = :type and p.uuid in (select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref where ref.clusterUuid = :clusterUuid)";
        TypedQuery<PrimaryStorageVO> query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
        query.setParameter("type", NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        query.setParameter("clusterUuid", host.getClusterUuid());
        return query.getResultList();
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        List<PrimaryStorageVO> ps = getPrimaryStorageHostShouldMount(inv);
        if (ps.isEmpty()) {
            return;
        }
        FutureCompletion compl = new FutureCompletion(null);

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(ps).each((pvo, completion) -> {
            mount(PrimaryStorageInventory.valueOf(pvo), inv.getUuid(), new Completion(completion){

                @Override
                public void success() {
                    completion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errs.add(errorCode);
                    completion.done();
                }
            });

        }).run(new WhileDoneCompletion(compl) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if(!errs.isEmpty()){
                    compl.fail(errs.get(0));
                }else {
                    compl.success();
                }
            }
        });

        compl.await();

        if (!compl.isSuccess()) {
            throw new OperationFailureException(compl.getErrorCode());
        }
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void deleteImageCache(ImageCacheInventory imageCache) {
    }


    private void delete(final PrimaryStorageInventory pinv, final String installPath, boolean isFolder, final Completion completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);
        DeleteCmd cmd = new DeleteCmd();
        cmd.setFolder(isFolder);
        cmd.setInstallPath(installPath);
        cmd.setUuid(pinv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(DELETE_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                DeleteResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(DeleteResponse.class);
                if (!rsp.isSuccess()) {
                    logger.warn(String.format("failed to delete bits[%s] on nfs primary storage[uuid:%s], %s, will clean up",
                            installPath, pinv.getUuid(), rsp.getError()));
                    completion.fail(operr("failed to delete bits[%s] on nfs primary storage[uuid:%s], %s, will clean up " +
                                    "installPath, pinv.getUuid(), rsp.getError()",
                            installPath, pinv.getUuid(), rsp.getError()));
                    return;
                }

                nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);

                completion.success();
            }
        });
    }

    @Override
    public void delete(final PrimaryStorageInventory pinv, final String installPath, final Completion completion) {
        delete(pinv, installPath, false, completion);
    }

    @Override
    public void deleteFolder(PrimaryStorageInventory pinv, String installPath, Completion completion) {
        delete(pinv, installPath, true, completion);
    }

    @Override
    public void revertVolumeFromSnapshot(VolumeSnapshotInventory sinv, VolumeInventory vol, HostInventory host, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion) {
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.setSnapshotInstallPath(sinv.getPrimaryStorageInstallPath());
        cmd.setUuid(sinv.getPrimaryStorageUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(REVERT_VOLUME_FROM_SNAPSHOT_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                RevertVolumeFromSnapshotResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(RevertVolumeFromSnapshotResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to revert volume[uuid:%s] to snapshot[uuid:%s] on kvm host[uuid:%s, ip:%s], %s",
                            vol.getUuid(), sinv.getUuid(), host.getUuid(), host.getManagementIp(), rsp.getError()));
                    return;
                }

                RevertVolumeFromSnapshotOnPrimaryStorageReply r = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                r.setNewVolumeInstallPath(rsp.getNewVolumeInstallPath());
                r.setSize(rsp.getSize());
                completion.success(r);
            }
        });
    }


    @Override
    public void resetRootVolumeFromImage(final VolumeInventory vol, final HostInventory host, final ReturnValueCompletion<String> completion) {
        ReInitImageCmd cmd = new ReInitImageCmd();
        PrimaryStorageInventory psInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(vol.getPrimaryStorageUuid(), PrimaryStorageVO.class));
        cmd.setImagePath(NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrlFromImageUuidForTemplate(psInv, vol.getRootImageUuid()));
        cmd.setVolumePath(NfsPrimaryStorageKvmHelper.makeRootVolumeInstallUrl(psInv, vol));
        cmd.setUuid(vol.getPrimaryStorageUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(REINIT_IMAGE_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                ReInitImageRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(ReInitImageRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to revert volume[uuid:%s] to image[uuid:%s] on kvm host[uuid:%s, ip:%s], %s",
                                    vol.getUuid(), vol.getRootImageUuid(), host.getUuid(), host.getManagementIp(), rsp.getError()));
                    return;
                }

                completion.success(rsp.getNewVolumeInstallPath());
            }
        });
    }

    @Override
    public void createVolumeFromImageCache(final PrimaryStorageInventory primaryStorage, final ImageCacheInventory image,
                                           final VolumeInventory volume, final ReturnValueCompletion<String> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(primaryStorage).get(0);

        final String installPath = StringUtils.isNotEmpty(volume.getInstallPath()) ? volume.getInstallPath() :
                NfsPrimaryStorageKvmHelper.makeRootVolumeInstallUrl(primaryStorage, volume);
        final String accountUuid = acntMgr.getOwnerAccountUuidOfResource(volume.getUuid());
        final CreateRootVolumeFromTemplateCmd cmd = new CreateRootVolumeFromTemplateCmd();
        cmd.setTemplatePathInCache(image.getInstallUrl());
        cmd.setInstallUrl(installPath);
        cmd.setAccountUuid(accountUuid);
        cmd.setName(volume.getName());
        cmd.setVolumeUuid(volume.getUuid());
        cmd.setUuid(primaryStorage.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(CREATE_VOLUME_FROM_TEMPLATE_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CreateRootVolumeFromTemplateResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(CreateRootVolumeFromTemplateResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("fails to create root volume[uuid:%s] from cached image[path:%s] because %s",
                            volume.getUuid(), image.getImageUuid(), rsp.getError());
                    completion.fail(err);
                    return;
                }


                nfsMgr.reportCapacityIfNeeded(primaryStorage.getUuid(), rsp);
                completion.success(installPath);
            }
        });
    }

    @Override
    public void createImageCacheFromVolumeResource(PrimaryStorageInventory primaryStorage, String volumeResource, ImageInventory image, ReturnValueCompletion<BitsInfo> completion) {
        final String installPath = NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrl(primaryStorage, image);
        doCreateTemplateFromVolume(installPath, primaryStorage, volumeResource, image, completion);
    }

    @Override
    public void createTemplateFromVolume(final PrimaryStorageInventory primaryStorage, final VolumeInventory volume, final ImageInventory image, final ReturnValueCompletion<BitsInfo> completion) {
        final String installPath = NfsPrimaryStorageKvmHelper.makeTemplateFromVolumeInWorkspacePath(primaryStorage, image.getUuid());
        doCreateTemplateFromVolume(installPath, primaryStorage, volume.getInstallPath(), image, completion);
    }

    private void doCreateTemplateFromVolume(final String installPath, final PrimaryStorageInventory primaryStorage, final String volumeResourceInstallPath, final ImageInventory image, final ReturnValueCompletion<BitsInfo> completion) {
        final HostInventory destHost = nfsFactory.getConnectedHostForOperation(primaryStorage).get(0);

        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
        cmd.setInstallPath(installPath);
        cmd.setVolumePath(volumeResourceInstallPath);
        cmd.setUuid(primaryStorage.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(destHost.getUuid());
        msg.setPath(CREATE_TEMPLATE_FROM_VOLUME_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHost.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CreateTemplateFromVolumeRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(CreateTemplateFromVolumeRsp.class);
                if (!rsp.isSuccess()) {
                    String sb = String.format("failed to create template from volume, because %s", rsp.getError()) +
                            String.format("\ntemplate:%s", JSONObjectUtil.toJsonString(image)) +
                            String.format("\nvolume resource:%s", volumeResourceInstallPath) +
                            String.format("\nnfs primary storage uuid:%s", primaryStorage.getUuid()) +
                            String.format("\nKVM host uuid:%s, management ip:%s", destHost.getUuid(), destHost.getManagementIp());
                    completion.fail(operr(sb));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("successfully created template from volumes"));
                sb.append(String.format("\ntemplate:%s", JSONObjectUtil.toJsonString(image)));
                sb.append(String.format("\nvolume resource:%s", volumeResourceInstallPath));
                sb.append(String.format("\nnfs primary storage uuid:%s", primaryStorage.getUuid()));
                sb.append(String.format("\nKVM host uuid:%s, management ip:%s", destHost.getUuid(), destHost.getManagementIp()));

                logger.debug(sb.toString());
                nfsMgr.reportCapacityIfNeeded(primaryStorage.getUuid(), rsp);
                completion.success(new BitsInfo(installPath, rsp.getSize(), rsp.getActualSize()));
            }
        });
    }

    @Override
    public void mergeSnapshotToVolume(final PrimaryStorageInventory pinv, VolumeSnapshotInventory snapshot,
                                      VolumeInventory volume, boolean fullRebase, final Completion completion) {
        if (volume.getType().equals(VolumeType.Memory.toString())) {
            completion.success();
            return;
        }

        boolean offline = true;
        String hostUuid = null;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            hostUuid = t.get(1, String.class);
            offline = (state == VmInstanceState.Stopped || state == VmInstanceState.Destroyed);
        }

        if (offline) {
            HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);

            OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
            cmd.setFullRebase(fullRebase);
            cmd.setSrcPath(snapshot.getPrimaryStorageInstallPath());
            cmd.setDestPath(volume.getInstallPath());
            cmd.setUuid(pinv.getUuid());

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setPath(OFFLINE_SNAPSHOT_MERGE);
            msg.setHostUuid(host.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    OfflineMergeSnapshotRsp rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(OfflineMergeSnapshotRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                        return;
                    }

                    nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                    completion.success();
                }
            });
        } else {
            MergeVolumeSnapshotOnKvmMsg msg = new MergeVolumeSnapshotOnKvmMsg();
            msg.setFullRebase(fullRebase);
            msg.setHostUuid(hostUuid);
            msg.setFrom(snapshot);
            msg.setTo(volume);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }
                }
            });
        }
    }

    @Override
    public void remount(final PrimaryStorageInventory pinv, String clusterUuid, final Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        final List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            completion.fail(operr("no hosts in the cluster[uuid:%s] are connected", clusterUuid));
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(huuids).all((hostUuid, compl) -> {
            RemountCmd cmd = new RemountCmd();
            cmd.url = pinv.getUrl();
            cmd.mountPath = pinv.getMountPath();
            cmd.options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(pinv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

            asyncHttpCall(REMOUNT_PATH, hostUuid, cmd, NfsPrimaryStorageAgentResponse.class, pinv, new ReturnValueCompletion<NfsPrimaryStorageAgentResponse>(compl) {
                @Override
                public void success(NfsPrimaryStorageAgentResponse rsp) {
                    logger.debug(String.format("remount NFS primary storage[uuid:%s, name:%s] on the KVM host[uuid:%s],", pinv.getUuid(), pinv.getName(), hostUuid));
                    nfsFactory.updateNfsHostStatus(pinv.getUuid(), hostUuid, PrimaryStorageHostStatus.Connected);
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    nfsFactory.updateNfsHostStatus(pinv.getUuid(), hostUuid, PrimaryStorageHostStatus.Disconnected);
                    errs.add(errorCode);

                    logger.warn(String.format("failed to remount NFS primary storage[uuid:%s, name:%s] on the KVM host[uuid:%s]," +
                            "%s.", pinv.getUuid(), pinv.getName(), hostUuid, errorCode.toString()));
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if(!errs.isEmpty()){
                    completion.fail(errs.get(0));
                }else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public void updateMountPoint(PrimaryStorageInventory pinv, String clusterUuid, String oldMountPoint,
                                 String newMountPoint, Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        final List<HostVO> hosts = q.list();
        if (hosts.isEmpty()) {
            completion.success();
            return;
        }

        String options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(pinv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

        new LoopAsyncBatch<String>(completion) {
            @Override
            protected Collection<String> collect() {
                return hosts.stream().map(HostVO::getUuid).collect(Collectors.toList());
            }

            @Override
            protected AsyncBatchRunner forEach(String hostUuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        UpdateMountPointCmd cmd = new UpdateMountPointCmd();
                        cmd.mountPath = pinv.getMountPath();
                        cmd.newMountPoint = newMountPoint;
                        cmd.oldMountPoint = oldMountPoint;
                        cmd.options = options;

                        asyncHttpCall(UPDATE_MOUNT_POINT_PATH, hostUuid, cmd, true, UpdateMountPointRsp.class, pinv,
                                new ReturnValueCompletion<UpdateMountPointRsp>(completion) {
                            @Override
                            public void success(UpdateMountPointRsp rsp) {
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errors.add(errorCode);

                                logger.warn(String.format("unable to update the nfs[uuid:%s, name:%s] mount point" +
                                                " from %s to %s on the host[uuid:%s], %s. Put the host-nfs into Disconnected status",
                                        pinv.getUuid(), pinv.getName(), oldMountPoint, newMountPoint, hostUuid, errorCode));

                                nfsFactory.updateNfsHostStatus(pinv.getUuid(), hostUuid, PrimaryStorageHostStatus.Disconnected);
                                completion.done();
                            }
                        });

                    }
                };
            }

            @Override
            protected void done() {
                if(errors.size() == hosts.size()){
                    completion.fail(errors.get(0));
                }else {
                    completion.success();
                }
            }
        }.start();
    }

    @Override
    public void handle(PrimaryStorageInventory pinv, ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> completion) {
        ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();

        String originType = msg.getVolume().getType();
        LinkVolumeNewDirCmd cmd = new LinkVolumeNewDirCmd();
        cmd.srcDir = NfsPrimaryStorageKvmHelper.makeVolumeInstallDir(pinv, msg.getVolume());
        msg.getVolume().setType(msg.getTargetType().toString());
        cmd.dstDir = NfsPrimaryStorageKvmHelper.makeVolumeInstallDir(pinv, msg.getVolume());
        msg.getVolume().setType(originType);
        cmd.volumeUuid = msg.getVolume().getUuid();
        cmd.setUuid(pinv.getUuid());

        if (!msg.getVolume().getInstallPath().startsWith(cmd.srcDir)) {
            completion.fail(operr("why volume[uuid:%s, installPath:%s] not in directory %s",
                    cmd.volumeUuid, msg.getVolume().getInstallPath(), cmd.srcDir));
            return;
        }

        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);
        asyncHttpCall(HARD_LINK_VOLUME, host.getUuid(), cmd, LinkVolumeNewDirRsp.class, pinv, new ReturnValueCompletion<LinkVolumeNewDirRsp>(completion) {
            @Override
            public void success(LinkVolumeNewDirRsp rsp) {
                VolumeInventory vol = msg.getVolume();
                String newPath = vol.getInstallPath().replace(cmd.srcDir, cmd.dstDir);
                vol.setInstallPath(newPath);
                reply.setVolume(vol);

                for (VolumeSnapshotInventory snapshot : msg.getSnapshots()) {
                    newPath = snapshot.getPrimaryStorageInstallPath().replace(cmd.srcDir, cmd.dstDir);
                    snapshot.setPrimaryStorageInstallPath(newPath);
                }
                reply.getSnapshots().addAll(msg.getSnapshots());
                reply.setInstallPathToGc(cmd.srcDir);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory pinv, GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion) {
        GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv).get(0);
        GetQcow2HashValueCmd cmd = new GetQcow2HashValueCmd();
        cmd.setInstallPath(msg.getPrimaryStorageInstallPath());
        cmd.setHostUuid(host.getUuid());
        asyncHttpCall(GET_QCOW2_HASH_VALUE_PATH, host.getUuid(), cmd, GetQcow2HashValueRsp.class, pinv, new ReturnValueCompletion<GetQcow2HashValueRsp>(completion) {

            @Override
            public void success(GetQcow2HashValueRsp rsp) {
                reply.setSnapshotUuid(msg.getSnapshotUuid());
                reply.setEncrypt(rsp.getHashValue());
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "remount-nfs-primary-storage";

            List<PrimaryStorageInventory> invs = getPrimaryStorageForHost(context.getInventory().getClusterUuid());

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                if (invs.isEmpty()) {
                    trigger.next();
                    return;
                }
                String huuid = context.getInventory().getUuid();

                if (context.isNewAddedHost() && !CoreGlobalProperty.UNIT_TEST_ON && !invs.isEmpty()) {
                    checkQemuImgVersionInOtherClusters(context, invs);
                }

                ErrorCodeList errList = (ErrorCodeList) data.get(KVMConstant.CONNECT_HOST_PRIMARYSTORAGE_ERROR);
                new While<>(invs).all((PrimaryStorageInventory inv, WhileCompletion completion) -> {
                    RemountCmd cmd = new RemountCmd();
                    cmd.mountPath = inv.getMountPath();
                    cmd.url = inv.getUrl();
                    cmd.options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(inv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

                    asyncHttpCall(REMOUNT_PATH, huuid, cmd, true, NfsPrimaryStorageAgentResponse.class, inv, new ReturnValueCompletion<NfsPrimaryStorageAgentResponse>(completion) {

                        @Override
                        public void success(NfsPrimaryStorageAgentResponse rsp) {
                            nfsFactory.updateNfsHostStatus(inv.getUuid(), huuid, PrimaryStorageHostStatus.Connected);
                            logger.debug(String.format("succeed to mount nfs[uuid:%s] from host[uuid:%s]"
                                    , inv.getUuid(), huuid));
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            logger.warn(String.format("fail to mount nfs[uuid:%s] from host[uuid:%s], because:%s"
                                    , inv.getUuid(), huuid, errorCode.toString()));
                            nfsFactory.updateNfsHostStatus(inv.getUuid(), huuid, PrimaryStorageHostStatus.Disconnected);
                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        data.put(KVMConstant.CONNECT_HOST_PRIMARYSTORAGE_ERROR, errList);
                        trigger.next();
                    }
                });
            }
        };
    }

    protected <T extends NfsPrimaryStorageAgentResponse> void asyncHttpCall(String path, final String hostUuid, NfsPrimaryStorageAgentCommand cmd, final Class<T> rspType, PrimaryStorageInventory inv, final ReturnValueCompletion<T> completion) {
        asyncHttpCall(path, hostUuid, cmd, false, rspType, inv, completion);
    }

    protected <T extends NfsPrimaryStorageAgentResponse> void asyncHttpCall(String path, final String hostUuid, NfsPrimaryStorageAgentCommand cmd, boolean noCheckStatus, final Class<T> rspType, PrimaryStorageInventory inv, final ReturnValueCompletion<T> completion) {
        cmd.setUuid(inv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                updatePrimaryStorageCapacity(inv.getUuid(), rsp);

                completion.success(rsp);
            }
        });
    }

    private <T extends NfsPrimaryStorageAgentResponse> void syncHttpCall(String path, final String hostUuid, NfsPrimaryStorageAgentCommand cmd, boolean noCheckStatus, final Class<T> rspType, PrimaryStorageInventory inv, final ReturnValueCompletion<T> completion) {
        cmd.setUuid(inv.getUuid());

        KVMHostSyncHttpCallMsg msg = new KVMHostSyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostSyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                updatePrimaryStorageCapacity(inv.getUuid(), rsp);

                completion.success(rsp);
            }
        });
    }

    private void updatePrimaryStorageCapacity(String psUuid, NfsPrimaryStorageAgentResponse rsp){
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            new PrimaryStorageCapacityUpdater(psUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
                @Override
                public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                    cap.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                    cap.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
                    cap.setTotalCapacity(rsp.getTotalCapacity());
                    cap.setSystemUsedCapacity(null);
                    return cap;
                }
            });
        }
    }

    @Override
    public void handle(PrimaryStorageInventory psInventory, AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();

        reply.setSnapshotInstallPath(NfsPrimaryStorageKvmHelper.makeKvmSnapshotInstallPath(psInventory, msg.getVolumeInventory(), msg.getSnapshotUuid()));
        completion.success(reply);
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (spec.getMemorySnapshotUuid() == null) {
            return;
        }

        VolumeSnapshotVO vo = dbf.findByUuid(spec.getMemorySnapshotUuid(), VolumeSnapshotVO.class);
        if (!Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)
                .eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).isExists()) {
            return;
        }

        cmd.setMemorySnapshotPath(vo.getPrimaryStorageInstallPath());
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
