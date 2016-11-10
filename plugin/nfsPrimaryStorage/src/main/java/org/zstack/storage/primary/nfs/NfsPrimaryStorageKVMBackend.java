package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.logging.Log;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.kvm.*;
import org.zstack.storage.primary.ChangePrimaryStorageStatusMsg;
import org.zstack.storage.primary.PrimaryStorageBase.PhysicalCapacityUsage;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class NfsPrimaryStorageKVMBackend implements NfsPrimaryStorageBackend,
        KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorageKVMBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private NfsPrimaryStorageFactory nfsFactory;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;

    public static final String MOUNT_PRIMARY_STORAGE_PATH = "/nfsprimarystorage/mount";
    public static final String UNMOUNT_PRIMARY_STORAGE_PATH = "/nfsprimarystorage/unmount";
    public static final String CREATE_EMPTY_VOLUME_PATH = "/nfsprimarystorage/createemptyvolume";
    public static final String GET_CAPACITY_PATH = "/nfsprimarystorage/getcapacity";
    public static final String DELETE_PATH = "/nfsprimarystorage/delete";
    public static final String CHECK_BITS_PATH = "/nfsprimarystorage/checkbits";
    public static final String MOVE_BITS_PATH = "/nfsprimarystorage/movebits";
    public static final String MERGE_SNAPSHOT_PATH = "/nfsprimarystorage/mergesnapshot";
    public static final String REBASE_MERGE_SNAPSHOT_PATH = "/nfsprimarystorage/rebaseandmergesnapshot";
    public static final String REVERT_VOLUME_FROM_SNAPSHOT_PATH = "/nfsprimarystorage/revertvolumefromsnapshot";
    public static final String CREATE_TEMPLATE_FROM_VOLUME_PATH = "/nfsprimarystorage/sftp/createtemplatefromvolume";
    public static final String OFFLINE_SNAPSHOT_MERGE = "/nfsprimarystorage/offlinesnapshotmerge";
    public static final String REMOUNT_PATH = "/nfsprimarystorage/remount";
    public static final String GET_VOLUME_SIZE_PATH = "/nfsprimarystorage/getvolumesize";
    public static final String PING_PATH = "/nfsprimarystorage/ping";
    public static final String GET_VOLUME_BASE_IMAGE_PATH = "/nfsprimarystorage/getvolumebaseimage";
    public static final String UPDATE_MOUNT_POINT_PATH = "/nfsprimarystorage/updatemountpoint";

    //////////////// For unit test //////////////////////////
    private boolean syncGetCapacity = false;
    public static final String SYNC_GET_CAPACITY_PATH = "/nfsprimarystorage/syncgetcapacity";
    //////////////// End for unit test //////////////////////

    private static final String QCOW3_QEMU_IMG_VERSION = "2.0.0";

    private void mount(PrimaryStorageInventory inv, String hostUuid) {
        MountCmd cmd = new MountCmd();
        cmd.setUrl(inv.getUrl());
        cmd.setMountPath(inv.getMountPath());
        cmd.setUuid(inv.getUuid());
        cmd.setOptions(NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(inv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN));

        KVMHostSyncHttpCallMsg msg = new KVMHostSyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(MOUNT_PRIMARY_STORAGE_PATH);
        msg.setHostUuid(hostUuid);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        MessageReply reply = bus.call(msg);

        if (!reply.isSuccess()) {
            ErrorCode err = reply.getError();

            if (reply.getError().getDetails().contains("java.net.SocketTimeoutException: Read timed out")) {
                // socket read timeout is caused by timeout of mounting a wrong URL
                err = errf.instantiateErrorCode(SysErrors.TIMEOUT, String.format("mount timeout. Please the check if the URL[%s] is" +
                        " valid to mount", inv.getUrl()), reply.getError());
            }

            throw new OperationFailureException(err);
        }

        MountAgentResponse rsp = ((KVMHostSyncHttpCallReply) reply).toResponse(MountAgentResponse.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }

        new PrimaryStorageCapacityUpdater(inv.getUuid()).update(
                rsp.getTotalCapacity(), rsp.getAvailableCapacity(), rsp.getTotalCapacity(), rsp.getAvailableCapacity()
        );

        logger.debug(String.format(
                "Successfully mounted nfs primary storage[uuid:%s] on kvm host[uuid:%s]",
                inv.getUuid(), hostUuid));
    }

    @Override
    public boolean attachToCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            checkQemuImgVersionInOtherClusters(inv, clusterUuid);
        }

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.select(HostVO_.uuid);
        query.add(HostVO_.state, Op.NOT_IN, HostState.PreMaintenance, HostState.Maintenance);
        query.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        query.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        List<String> hostUuids = query.listValue();
        for (String huuid : hostUuids) {
            mount(inv, huuid);
        }

        return !hostUuids.isEmpty();
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
                String err = String.format(
                        "unable to attach a primary storage[uuid:%s, name:%s] to cluster[uuid:%s]. Kvm host in the cluster has qemu-img "
                                + "with version[%s]; but the primary storage has attached to another cluster that has kvm host which has qemu-img with "
                                + "version[%s]. qemu-img version greater than %s is incompatible with versions less than %s, this will causes volume snapshot operation "
                                + "to fail. Please avoid attaching a primary storage to clusters that have different Linux distributions, in order to prevent qemu-img version mismatch",
                        inv.getUuid(), inv.getName(), clusterUuid, versionInCluster, otherVersion, QCOW3_QEMU_IMG_VERSION, QCOW3_QEMU_IMG_VERSION
                );

                throw new OperationFailureException(errf.stringToOperationError(err));
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
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void ping(PrimaryStorageInventory inv, final Completion completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
        PingCmd cmd = new PingCmd();
        cmd.setUuid(inv.getUuid());

        new KvmCommandSender(host.getUuid()).send(cmd, PING_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                NfsPrimaryStorageAgentResponse rsp = wrapper.getResponse(NfsPrimaryStorageAgentResponse.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, CreateTemporaryVolumeFromSnapshotMsg msg, final ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
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
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
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
    public void handle(PrimaryStorageInventory inv, CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
        VolumeSnapshotInventory sp = msg.getSnapshot();
        final String workspaceInstallPath = NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(inv, msg.getVolumeUuid());

        MergeSnapshotCmd cmd = new MergeSnapshotCmd();
        cmd.setSnapshotInstallPath(sp.getPrimaryStorageInstallPath());
        cmd.setWorkspaceInstallPath(workspaceInstallPath);
        cmd.setUuid(inv.getUuid());
        cmd.setVolumeUuid(sp.getVolumeUuid());

        new KvmCommandSender(host.getUuid()).send(cmd, MERGE_SNAPSHOT_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                MergeSnapshotResponse rsp = wrapper.getResponse(MergeSnapshotResponse.class);
                reply.setActualSize(rsp.getActualSize());
                reply.setSize(rsp.getSize());
                reply.setInstallPath(workspaceInstallPath);
                completion.success(reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handle(PrimaryStorageInventory inv, UploadBitsToBackupStorageMsg msg, final ReturnValueCompletion<UploadBitsToBackupStorageReply> completion) {
        BackupStorageVO bs = dbf.findByUuid(msg.getBackupStorageUuid(), BackupStorageVO.class);

        NfsPrimaryToBackupStorageMediator m = nfsFactory.getPrimaryToBackupStorageMediator(BackupStorageType.valueOf(bs.getType()), HypervisorType.valueOf(msg.getHypervisorType()));
        m.uploadBits(inv, BackupStorageInventory.valueOf(bs), msg.getBackupStorageInstallPath(), msg.getPrimaryStorageInstallPath(), new ReturnValueCompletion<String>(completion) {
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
        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
        KvmCommandSender sender = new KvmCommandSender(host.getUuid());

        GetVolumeActualSizeCmd cmd = new GetVolumeActualSizeCmd();
        cmd.setUuid(inv.getUuid());
        cmd.installPath = msg.getInstallPath();
        cmd.volumeUuid = msg.getVolumeUuid();
        sender.send(cmd, GET_VOLUME_SIZE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeActualSizeRsp rsp = wrapper.getResponse(GetVolumeActualSizeRsp.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
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
        cmd.volumeUUid = msg.getVolume().getUuid();
        cmd.installPath = msg.getVolume().getInstallPath();

        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
        new KvmCommandSender(host.getUuid()).send(cmd, GET_VOLUME_BASE_IMAGE_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                GetVolumeBaseImagePathRsp rsp = wrapper.getResponse(GetVolumeBaseImagePathRsp.class);
                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
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
    public void getPhysicalCapacity(PrimaryStorageInventory inv, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv);

        GetCapacityCmd cmd = new GetCapacityCmd();
        cmd.setMountPath(inv.getMountPath());
        cmd.setUuid(inv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(host.getUuid());
        msg.setPath(GET_CAPACITY_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    completion.fail(errf.stringToOperationError(rsp.getError()));
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
        HostInventory host = nfsFactory.getConnectedHostForOperation(inv);
        CheckIsBitsExistingCmd cmd = new CheckIsBitsExistingCmd();
        cmd.setUuid(inv.getUuid());
        cmd.setInstallPath(installPath);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to check existence of %s on nfs primary storage[uuid:%s], %s",
                                    installPath, inv.getUuid(), rsp.getError())
                    ));
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
                String err = String.format(
                        "unable to attach a primary storage to cluster. Kvm host[uuid:%s, name:%s] in cluster has qemu-img "
                                + "with version[%s]; but the primary storage has attached to a cluster that has kvm host[uuid:%s], which has qemu-img with "
                                + "version[%s]. qemu-img version greater than %s is incompatible with versions less than %s, this will causes volume snapshot operation "
                                + "to fail. Please avoid attaching a primary storage to clusters that have different Linux distributions, in order to prevent qemu-img version mismatch",
                        context.getInventory().getUuid(), context.getInventory().getName(), mine, e.getKey(), version, QCOW3_QEMU_IMG_VERSION, QCOW3_QEMU_IMG_VERSION
                );
                throw new OperationFailureException(errf.stringToOperationError(err));
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
        if (volume.getRootImageUuid() != null) {
            cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeRootVolumeInstallUrl(pinv, volume));
        } else {
            cmd.setInstallUrl(NfsPrimaryStorageKvmHelper.makeDataVolumeInstallUrl(pinv, volume.getUuid()));
        }

        final HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(CREATE_EMPTY_VOLUME_PATH);
        msg.setHostUuid(host.getUuid());
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    String err = String.format("unable to create empty volume[uuid:%s,  name:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            volume.getUuid(), volume.getName(), host.getUuid(), host.getManagementIp(), rsp.getError());
                    logger.warn(err);
                    complete.fail(errf.stringToOperationError(err));
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

        for (PrimaryStorageVO pvo : ps) {
            mount(PrimaryStorageInventory.valueOf(pvo), inv.getUuid());
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
        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);
        DeleteCmd cmd = new DeleteCmd();
        cmd.setFolder(isFolder);
        cmd.setInstallPath(installPath);
        cmd.setUuid(pinv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(DELETE_PATH);
        msg.setHostUuid(host.getUuid());
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                } else {
                    nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                }

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
    public void revertVolumeFromSnapshot(final VolumeSnapshotInventory sinv, final VolumeInventory vol, final HostInventory host, final ReturnValueCompletion<String> completion) {
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        cmd.setSnapshotInstallPath(sinv.getPrimaryStorageInstallPath());
        cmd.setUuid(sinv.getPrimaryStorageUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(REVERT_VOLUME_FROM_SNAPSHOT_PATH);
        msg.setHostUuid(host.getUuid());
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to revert volume[uuid:%s] to snapshot[uuid:%s] on kvm host[uuid:%s, ip:%s], %s",
                                    vol.getUuid(), sinv.getUuid(), host.getUuid(), host.getManagementIp(), rsp.getError())
                    ));
                    return;
                }

                completion.success(rsp.getNewVolumeInstallPath());
            }
        });
    }

    @Override
    public void resetRootVolumeFromImage(final VolumeInventory vol, final HostInventory host, final ReturnValueCompletion<String> completion) {
        RevertVolumeFromSnapshotCmd cmd = new RevertVolumeFromSnapshotCmd();
        PrimaryStorageInventory psInv = PrimaryStorageInventory.valueOf(dbf.findByUuid(vol.getPrimaryStorageUuid(), PrimaryStorageVO.class));
        cmd.setSnapshotInstallPath(NfsPrimaryStorageKvmHelper.makeCachedImageInstallUrlFromImageUuidForTemplate(psInv, vol.getRootImageUuid()));
        cmd.setUuid(vol.getPrimaryStorageUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(REVERT_VOLUME_FROM_SNAPSHOT_PATH);
        msg.setHostUuid(host.getUuid());
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to revert volume[uuid:%s] to image[uuid:%s] on kvm host[uuid:%s, ip:%s], %s",
                                    vol.getUuid(), vol.getRootImageUuid(), host.getUuid(), host.getManagementIp(), rsp.getError())
                    ));
                    return;
                }

                completion.success(rsp.getNewVolumeInstallPath());
            }
        });
    }

    @Override
    public void createTemplateFromVolume(final PrimaryStorageInventory primaryStorage, final VolumeInventory volume, final ImageInventory image, final ReturnValueCompletion<String> completion) {
        final HostInventory destHost = nfsFactory.getConnectedHostForOperation(primaryStorage);

        final String installPath = NfsPrimaryStorageKvmHelper.makeTemplateFromVolumeInWorkspacePath(primaryStorage, image.getUuid());
        CreateTemplateFromVolumeCmd cmd = new CreateTemplateFromVolumeCmd();
        cmd.setInstallPath(installPath);
        cmd.setVolumePath(volume.getInstallPath());
        cmd.setUuid(primaryStorage.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(destHost.getUuid());
        msg.setPath(CREATE_TEMPLATE_FROM_VOLUME_PATH);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("failed to create template from volume, because %s", rsp.getError()));
                    sb.append(String.format("\ntemplate:%s", JSONObjectUtil.toJsonString(image)));
                    sb.append(String.format("\nvolume:%s", JSONObjectUtil.toJsonString(volume)));
                    sb.append(String.format("\nnfs primary storage uuid:%s", primaryStorage.getUuid()));
                    sb.append(String.format("\nKVM host uuid:%s, management ip:%s", destHost.getUuid(), destHost.getManagementIp()));
                    completion.fail(errf.stringToOperationError(sb.toString()));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("successfully created template from volumes"));
                sb.append(String.format("\ntemplate:%s", JSONObjectUtil.toJsonString(image)));
                sb.append(String.format("\nvolume:%s", JSONObjectUtil.toJsonString(volume)));
                sb.append(String.format("\nnfs primary storage uuid:%s", primaryStorage.getUuid()));
                sb.append(String.format("\nKVM host uuid:%s, management ip:%s", destHost.getUuid(), destHost.getManagementIp()));

                logger.debug(sb.toString());
                nfsMgr.reportCapacityIfNeeded(primaryStorage.getUuid(), rsp);
                completion.success(installPath);
            }
        });
    }

    @Override
    public void mergeSnapshotToVolume(final PrimaryStorageInventory pinv, VolumeSnapshotInventory snapshot,
                                      VolumeInventory volume, boolean fullRebase, final Completion completion) {
        boolean offline = true;
        String hostUuid = null;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state, VmInstanceVO_.hostUuid);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            Tuple t = q.findTuple();
            VmInstanceState state = t.get(0, VmInstanceState.class);
            hostUuid = t.get(1, String.class);
            offline = (state == VmInstanceState.Stopped);
        }

        if (offline) {
            HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);

            OfflineMergeSnapshotCmd cmd = new OfflineMergeSnapshotCmd();
            cmd.setFullRebase(fullRebase);
            cmd.setSrcPath(snapshot.getPrimaryStorageInstallPath());
            cmd.setDestPath(volume.getInstallPath());
            cmd.setUuid(pinv.getUuid());

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setPath(OFFLINE_SNAPSHOT_MERGE);
            msg.setHostUuid(host.getUuid());
            msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
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
                        completion.fail(errf.stringToOperationError(rsp.getError()));
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
            completion.success();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = CollectionUtils.transformToList(huuids, new Function<KVMHostAsyncHttpCallMsg, String>() {
            @Override
            public KVMHostAsyncHttpCallMsg call(String arg) {
                RemountCmd cmd = new RemountCmd();
                cmd.setUuid(pinv.getUuid());
                cmd.url = pinv.getUrl();
                cmd.mountPath = pinv.getMountPath();
                cmd.options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(pinv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                msg.setHostUuid(arg);
                msg.setPath(REMOUNT_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            private void reconnectHost(String huuid, ErrorCode error) {
                logger.warn(String.format("failed to remount NFS primary storage[uuid:%s, name:%s] on the KVM host[uuid:%s]," +
                        "%s. Start a reconnect to fix the problem", pinv.getUuid(), pinv.getName(), huuid, error));

                ReconnectHostMsg rmsg = new ReconnectHostMsg();
                rmsg.setHostUuid(huuid);
                bus.makeTargetServiceIdByResourceUuid(rmsg, HostConstant.SERVICE_ID, huuid);
                bus.send(rmsg);
            }

            @Override
            public void run(List<MessageReply> replies) {
                boolean reported = false;
                List<ErrorCode> errors = new ArrayList<ErrorCode>();
                boolean success = false;

                for (MessageReply re : replies) {
                    String huuid = huuids.get(replies.indexOf(re));

                    if (!re.isSuccess()) {
                        errors.add(re.getError());
                        reconnectHost(huuid, re.getError());
                        continue;
                    }

                    KVMHostAsyncHttpCallReply ar = re.castReply();
                    NfsPrimaryStorageAgentResponse rsp = ar.toResponse(NfsPrimaryStorageAgentResponse.class);
                    if (!rsp.isSuccess()) {
                        ErrorCode err = errf.stringToOperationError(rsp.getError());
                        errors.add(err);
                        reconnectHost(huuid, err);
                        continue;
                    }

                    success = true;
                    if (!reported) {
                        new PrimaryStorageCapacityUpdater(pinv.getUuid()).update(
                                rsp.getTotalCapacity(), rsp.getAvailableCapacity(), rsp.getTotalCapacity(), rsp.getAvailableCapacity()
                        );
                        reported = true;
                    }
                }

                if (success) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(String.format("%s", errors)));
                }
            }
        });
    }

    @Override
    public void updateMountPoint(PrimaryStorageInventory pinv, String clusterUuid, String oldMountPoint,
                                 String newMountPoint, Completion completion) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
        q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        final List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            completion.success();
            return;
        }

        String options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(pinv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

        new LoopAsyncBatch<String>() {
            @Override
            protected Collection<String> collect() {
                return huuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String hostUuid) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        UpdateMountPointCmd cmd = new UpdateMountPointCmd();
                        cmd.setUuid(pinv.getUuid());
                        cmd.mountPath = pinv.getMountPath();
                        cmd.newMountPoint = newMountPoint;
                        cmd.oldMountPoint = oldMountPoint;
                        cmd.options = options;

                        new KvmCommandSender(hostUuid).send(cmd, UPDATE_MOUNT_POINT_PATH, new KvmCommandFailureChecker() {
                            @Override
                            public ErrorCode getError(KvmResponseWrapper wrapper) {
                                UpdateMountPointRsp rsp = wrapper.getResponse(UpdateMountPointRsp.class);
                                return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
                            }
                        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                            @Override
                            public void success(KvmResponseWrapper w) {
                                UpdateMountPointRsp rsp = w.getResponse(UpdateMountPointRsp.class);
                                new PrimaryStorageCapacityUpdater(pinv.getUuid()).update(
                                        rsp.getTotalCapacity(), rsp.getAvailableCapacity(), rsp.getTotalCapacity(), rsp.getAvailableCapacity()
                                );
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to update the nfs[uuid:%s, name:%s] mount point" +
                                                " from %s to %s in the cluster[uuid:%s], %s", pinv.getUuid(), pinv.getName(),
                                        oldMountPoint, newMountPoint, hostUuid, errorCode));
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                completion.success();
            }
        }.start();
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

                new Log(context.getInventory().getUuid()).log(NfsPrimaryStorageLabels.INIT);

                if (context.isNewAddedHost() && !CoreGlobalProperty.UNIT_TEST_ON && !invs.isEmpty()) {
                    checkQemuImgVersionInOtherClusters(context, invs);
                }

                List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<KVMHostAsyncHttpCallMsg>();
                for (PrimaryStorageInventory inv : invs) {
                    RemountCmd cmd = new RemountCmd();
                    cmd.mountPath = inv.getMountPath();
                    cmd.url = inv.getUrl();
                    cmd.setUuid(inv.getUuid());
                    cmd.options = NfsSystemTags.MOUNT_OPTIONS.getTokenByResourceUuid(inv.getUuid(), NfsSystemTags.MOUNT_OPTIONS_TOKEN);

                    KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                    msg.setCommand(cmd);
                    msg.setNoStatusCheck(true);
                    msg.setPath(REMOUNT_PATH);
                    msg.setHostUuid(context.getInventory().getUuid());
                    msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
                    bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                    msgs.add(msg);
                }

                bus.send(msgs, new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply reply : replies) {
                            if (!reply.isSuccess()) {
                                throw new OperationFailureException(reply.getError());
                            }

                            KVMHostAsyncHttpCallReply r = reply.castReply();
                            final NfsPrimaryStorageAgentResponse rsp = r.toResponse(NfsPrimaryStorageAgentResponse.class);

                            if (!rsp.isSuccess()) {
                                throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
                            }

                            PrimaryStorageInventory inv = invs.get(replies.indexOf(reply));
                            new PrimaryStorageCapacityUpdater(inv.getUuid()).run(new PrimaryStorageCapacityUpdaterRunnable() {
                                @Override
                                public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                                    if (cap.getTotalCapacity() == 0 && cap.getAvailableCapacity() == 0) {
                                        // init
                                        cap.setTotalCapacity(rsp.getTotalCapacity());
                                        cap.setAvailableCapacity(rsp.getAvailableCapacity());
                                    }

                                    cap.setTotalPhysicalCapacity(rsp.getTotalCapacity());
                                    cap.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());

                                    return cap;
                                }
                            });

                            if (!PrimaryStorageStatus.Connected.toString().equals(inv.getStatus())) {
                                // use sync call here to make sure the NFS primary storage connected before continue to the next step
                                ChangePrimaryStorageStatusMsg cmsg = new ChangePrimaryStorageStatusMsg();
                                cmsg.setPrimaryStorageUuid(inv.getUuid());
                                cmsg.setStatus(PrimaryStorageStatus.Connected.toString());
                                bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
                                bus.call(cmsg);
                            }
                        }

                        trigger.next();
                    }
                });
            }
        };
    }
}
