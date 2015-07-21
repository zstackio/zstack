package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.kvm.*;
import org.zstack.storage.primary.PrimaryStorageBase.PhysicalCapacityUsage;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.*;
import org.zstack.utils.Bucket;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class NfsPrimaryStorageKVMBackend implements NfsPrimaryStorageBackend,
        KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryStorageKVMBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
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

        KVMHostSyncHttpCallMsg msg = new KVMHostSyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(MOUNT_PRIMARY_STORAGE_PATH);
        msg.setHostUuid(hostUuid);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        MessageReply reply =  bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        MountAgentResponse rsp = ((KVMHostSyncHttpCallReply)reply).toResponse(MountAgentResponse.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }

        nfsMgr.reportCapacityIfNeeded(inv.getUuid(), rsp);

        PrimaryStorageReportCapacityMsg rmsg = new PrimaryStorageReportCapacityMsg();
        rmsg.setPrimaryStorageUuid(inv.getUuid());
        rmsg.setAvailableCapacity(rsp.getAvailableCapacity());
        rmsg.setTotalCapacity(rsp.getTotalCapacity());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
        bus.send(rmsg);

        logger.debug(String.format(
                "Successfully mounted nfs primary storage[uuid:%s] on kvm host[uuid:%s]",
                inv.getUuid(), hostUuid));
    }

    @Override
    public void attachToCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException {
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

        AgentResponse rsp = ((KVMHostSyncHttpCallReply)reply).toResponse(AgentResponse.class);
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
    public void getPhysicalCapacity(PrimaryStorageInventory inv, final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        final HostInventory host = nfsFactory.getConnectedHostForOperation(inv);

        GetCapacityCmd cmd = new GetCapacityCmd();
        cmd.setMountPath(inv.getMountPath());

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

                CheckIsBitsExistingRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(CheckIsBitsExistingRsp.class);
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

    @Transactional
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

    @Override
    public void kvmHostConnected(KVMHostConnectedContext context) throws KVMHostConnectException {
        List<PrimaryStorageInventory> invs = getPrimaryStorageForHost(context.getInventory().getClusterUuid());
        if (context.isNewAddedHost() && !CoreGlobalProperty.UNIT_TEST_ON && !invs.isEmpty()) {
            checkQemuImgVersionInOtherClusters(context, invs);
        }

        for (PrimaryStorageInventory inv : invs) {
            MountCmd cmd = new MountCmd();
            cmd.setUrl(inv.getUrl());
            cmd.setMountPath(inv.getMountPath());
            cmd.setUuid(inv.getUuid());
            MountAgentResponse rsp = restf.syncJsonPost(context.buildUrl(MOUNT_PRIMARY_STORAGE_PATH), cmd, MountAgentResponse.class);
            if (!rsp.isSuccess()) {
                throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
            }

            nfsMgr.reportCapacityIfNeeded(inv.getUuid(), rsp);

            PrimaryStorageReportCapacityMsg rmsg = new PrimaryStorageReportCapacityMsg();
            rmsg.setPrimaryStorageUuid(inv.getUuid());
            rmsg.setAvailableCapacity(rsp.getAvailableCapacity());
            rmsg.setTotalCapacity(rsp.getTotalCapacity());
            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
            bus.send(rmsg);
        }
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
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                CreateEmptyVolumeResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(CreateEmptyVolumeResponse.class);
                if (!rsp.isSuccess()) {
                    String err = String.format("unable to create empty volume[uuid:%s,  name:%s] on kvm host[uuid:%s, ip:%s], because %s",
                            volume.getUuid(), volume.getName(), host.getUuid(), host.getManagementIp(), rsp.getError());
                    logger.warn(err);
                    complete.fail(errf.stringToOperationError(err));
                    return;
                }

                volume.setInstallPath(cmd.getInstallUrl());

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
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                DeleteResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(DeleteResponse.class);
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
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                RevertVolumeFromSnapshotResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(RevertVolumeFromSnapshotResponse.class);
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
        msg.setCommandTimeout(NfsPrimaryStorageGlobalProperty.KVM_CreateTemplateFromVolumeCmd_TIMEOUT);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, destHost.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CreateTemplateFromVolumeRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(CreateTemplateFromVolumeRsp.class);
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

    private void createBitsFromVolumeSnapshot(final PrimaryStorageInventory pinv, List<SnapshotDownloadInfo> snapshots,
                                              final String bitsUuid, final String bitsName, boolean needDownload,
                                              final ReturnValueCompletion<CreateBitsFromSnapshotResult> completion) {
        if (!needDownload) {
            HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);
            final VolumeSnapshotInventory latest = snapshots.get(snapshots.size()-1).getSnapshot();
            final String workspaceInstallPath = NfsPrimaryStorageKvmHelper.makeSnapshotWorkspacePath(pinv, bitsUuid);
            MergeSnapshotCmd cmd = new MergeSnapshotCmd();
            cmd.setSnapshotInstallPath(latest.getPrimaryStorageInstallPath());
            cmd.setWorkspaceInstallPath(workspaceInstallPath);
            cmd.setUuid(pinv.getUuid());

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setPath(MERGE_SNAPSHOT_PATH);
            msg.setHostUuid(host.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    MergeSnapshotResponse rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(MergeSnapshotResponse.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(errf.stringToOperationError(
                                String.format("failed to create %s[uuid:%s] from snapshot[uuid:%s] on nfs primary storage[uuid:%s], %s",
                                        bitsName, bitsUuid, latest.getUuid(), pinv.getUuid(), rsp.getError())
                        ));
                        return;
                    }

                    CreateBitsFromSnapshotResult result = new CreateBitsFromSnapshotResult();
                    result.setInstallPath(workspaceInstallPath);
                    result.setSize(rsp.getSize());
                    completion.success(result);
                }
            });
        } else {
            downloadAndCreateBitsFromVolumeSnapshots(pinv, snapshots, bitsName, bitsUuid, completion);
        }
    }

    @Override
    public void createTemplateFromVolumeSnapshot(final PrimaryStorageInventory pinv, List<SnapshotDownloadInfo> snapshots,
                                                 final String imageUuid, boolean needDownload,
                                                 final ReturnValueCompletion<CreateBitsFromSnapshotResult> completion) {
        createBitsFromVolumeSnapshot(pinv, snapshots, imageUuid, "template", needDownload, completion);
    }

    @Override
    public void createDataVolumeFromVolumeSnapshot(PrimaryStorageInventory pinv,
                                                   List<SnapshotDownloadInfo> infos,
                                                   String volumeUuid, boolean needDownload,
                                                   ReturnValueCompletion<CreateBitsFromSnapshotResult> completion) {
        createBitsFromVolumeSnapshot(pinv, infos, volumeUuid, "volume", needDownload, completion);
    }

    @Override
    public void moveBits(final PrimaryStorageInventory pinv, String srcPath, String destPath, final Completion completion) {
        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);
        MoveBitsCmd cmd = new MoveBitsCmd();
        cmd.setSrcPath(srcPath);
        cmd.setDestPath(destPath);
        cmd.setUuid(pinv.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(MOVE_BITS_PATH);
        msg.setHostUuid(host.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                MoveBitsRsp rsp = hreply.toResponse(MoveBitsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(errf.stringToOperationError(rsp.getError()));
                    return;
                }

                nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                completion.success();
            }
        });
    }

    @Override
    public void mergeSnapshotToVolume(final PrimaryStorageInventory pinv, VolumeSnapshotInventory snapshot,
                                      VolumeInventory volume, boolean fullRebase, final Completion completion) {
        boolean offline = true;
        if (volume.getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q  = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, volume.getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            offline = (state == VmInstanceState.Stopped);
        }

        HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);

        if (offline) {
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

                    OfflineMergeSnapshotRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(OfflineMergeSnapshotRsp.class);
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
            msg.setHostUuid(host.getUuid());
            msg.setFrom(snapshot);
            msg.setTo(volume);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
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

    private void downloadAndCreateBitsFromVolumeSnapshots(final PrimaryStorageInventory pinv,
                                                          final List<SnapshotDownloadInfo> snapshots,
                                                          final String bitsName,
                                                          final String bitsUuid,
                                                          final ReturnValueCompletion<CreateBitsFromSnapshotResult> completion) {
        final HostInventory host = nfsFactory.getConnectedHostForOperation(pinv);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-merge-snapshot-on-nfs-primary-storage-%s", pinv.getUuid()));
        chain.then(new ShareFlow() {
            List<String> snapshotInstallPaths = new ArrayList<String>();
            String workspaceInstallPath = NfsPrimaryStorageKvmHelper.makeSnapshotWorkspacePath(pinv, bitsUuid);
            long templateSize;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "download-snapshot-from-backup-storage";

                    Map<String, Bucket> mediatorMap = new HashMap<String, Bucket>();

                    private Bucket findMediatorAndBackupStorage(SnapshotDownloadInfo info) {
                        Bucket ret = mediatorMap.get(info.getBackupStorageUuid());
                        if (ret == null) {
                            BackupStorageVO bsvo = dbf.findByUuid(info.getBackupStorageUuid(), BackupStorageVO.class);
                            BackupStorageInventory bsinv = BackupStorageInventory.valueOf(bsvo);

                            NfsPrimaryToBackupStorageMediator mediator = nfsFactory.getPrimaryToBackupStorageMediator(
                                    BackupStorageType.valueOf(bsinv.getType()),
                                    nfsMgr.findHypervisorTypeByImageFormatAndPrimaryStorageUuid(info.getSnapshot().getFormat(), pinv.getUuid())
                            );

                            ret = Bucket.newBucket(mediator, bsinv);
                            mediatorMap.put(info.getBackupStorageUuid(), ret);
                        }

                        return ret;
                    }

                    private void download(final Iterator<SnapshotDownloadInfo> it, final Completion completion1) {
                        if (!it.hasNext()) {
                            Collections.reverse(snapshotInstallPaths);
                            completion1.success();
                            return;
                        }

                        final SnapshotDownloadInfo info = it.next();
                        final VolumeSnapshotInventory sinv = info.getSnapshot();
                        Bucket bucket = findMediatorAndBackupStorage(info);
                        NfsPrimaryToBackupStorageMediator mediator = bucket.get(0);
                        final BackupStorageInventory bsinv = bucket.get(1);
                        final String installPath = NfsPrimaryStorageKvmHelper.makeSnapshotWorkspacePath(pinv, sinv.getUuid());

                        mediator.downloadBits(pinv, bsinv, info.getBackupStorageInstallPath(), installPath, new Completion(completion1) {
                            @Override
                            public void success() {
                                logger.debug(String.format("download volume snapshot[uuid:%s, name:%s] from backup storage[uuid:%s, %s] to nfs primary storage[uuid:%s, path:%s]",
                                        sinv.getUuid(), sinv.getName(), bsinv.getUuid(), info.getBackupStorageInstallPath(), pinv.getUuid(), installPath));
                                snapshotInstallPaths.add(installPath);
                                download(it, completion1);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion1.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        download(snapshots.iterator(), new Completion(trigger) {
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
                    public void rollback(FlowTrigger trigger, Map data) {
                        for (String installPath : snapshotInstallPaths) {
                            delete(pinv, installPath, new NopeCompletion());
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "merge-snapshot-on-primary-storage";

                    boolean mergeSuccess;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RebaseAndMergeSnapshotsCmd cmd = new RebaseAndMergeSnapshotsCmd();
                        cmd.setSnapshotInstallPaths(snapshotInstallPaths);
                        cmd.setWorkspaceInstallPath(workspaceInstallPath);
                        cmd.setUuid(pinv.getUuid());

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setPath(REBASE_MERGE_SNAPSHOT_PATH);
                        msg.setHostUuid(host.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                RebaseAndMergeSnapshotsResponse rsp = ((KVMHostAsyncHttpCallReply) reply).toResponse(RebaseAndMergeSnapshotsResponse.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(errf.stringToOperationError(
                                            String.format("failed to rebase and merge snapshots for %s[uuid:%s] on nfs primary storage[uuid:%s], %s",
                                                    bitsName, bitsUuid, pinv.getUuid(), rsp.getError())
                                    ));
                                    return;
                                }

                                nfsMgr.reportCapacityIfNeeded(pinv.getUuid(), rsp);
                                templateSize = rsp.getSize();
                                mergeSuccess = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        if (mergeSuccess) {
                            delete(pinv, workspaceInstallPath, new NopeCompletion());
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temporary-snapshot-in-workspace";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String installPath : snapshotInstallPaths) {
                            delete(pinv, installPath, new NopeCompletion());
                        }
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        CreateBitsFromSnapshotResult result = new CreateBitsFromSnapshotResult();
                        result.setSize(templateSize);
                        result.setInstallPath(workspaceInstallPath);
                        completion.success(result);
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
}
