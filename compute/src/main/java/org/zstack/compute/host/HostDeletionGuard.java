package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.BuildVolumeSpecExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostDeletionGuard {
    private static final String PREFIX = "hostDeletionGuard::";
    private static final String USING = PREFIX + "using::";
    private static final String DELETING = PREFIX + "deleting::";
    private static final String LEASES = HostDeletionGuard.class.getName();

    @Autowired
    private PluginRegistry pluginRgty;

    // Only admission is serialized. Leases do not hold database connections or
    // the host/VM task queues while asynchronous operations are running.
    public long acquire(String hostUuid, boolean deleting) {
        GLock lock = new GLock("host-delete-guard-" + hostUuid, 30);
        lock.lock();
        try {
            if (!Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).isExists()) {
                throw new OperationFailureException(operr("host[uuid:%s] no longer exists", hostUuid));
            }

            // UUIDs are reused on restart; the DB join date identifies the
            // current incarnation without relying on the caller's wall clock.
            List<Tuple> nodes = SQL.New("select mn.uuid, mn.joinDate from ManagementNodeVO mn", Tuple.class).list();
            Map<String, String> owners = new LinkedHashMap<>();
            nodes.forEach(mn -> owners.put(mn.get(0, String.class),
                    mn.get(0, String.class) + "::" + mn.get(1, Timestamp.class).getTime()));
            String owner = owners.get(Platform.getManagementServerId());
            if (owner == null) {
                throw new OperationFailureException(operr("management node is no longer available; retry the operation"));
            }
            // Losing a heartbeat record does not fence the old process or its
            // asynchronous work. Never reclaim its guards automatically.
            List<JsonLabelVO> blockers = deleting
                    ? Q.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, hostUuid)
                            .like(JsonLabelVO_.labelKey, PREFIX + "%").list()
                    : Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, DELETING + hostUuid).list();
            for (JsonLabelVO blocker : blockers) {
                if (!owners.containsValue(blocker.getLabelValue())) {
                    throw new OperationFailureException(operr(
                            "cannot use/delete host[uuid:%s]: unreleased operation guard[id:%s] belongs to an" +
                                    " unavailable or restarted management node. Verify that the old process and" +
                                    " its pending operations have stopped before manually removing the guard",
                            hostUuid, blocker.getId()));
                }
            }
            if (!blockers.isEmpty()) {
                throw new OperationFailureException(operr(deleting
                                ? "cannot delete host[uuid:%s]: a VM operation or another deletion is in progress; retry after it completes"
                                : "cannot use host[uuid:%s]: host deletion is in progress", hostUuid));
            }

            return new JsonLabel().create(deleting ? DELETING + hostUuid : USING + Platform.getUuid(),
                    owner, hostUuid).getId();
        } finally {
            lock.unlock();
        }
    }

    public static void release(long lease) {
        SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.id, lease).hardDelete();
    }

    public static boolean protectVm(VmInstanceSpec spec, String hostUuid, FlowTrigger trigger) {
        try {
            long lease = new HostDeletionGuard().acquire(hostUuid, false);
            // JsonWrapper snapshots values and deserializes generic JSON numbers
            // as doubles. Store IDs as strings and write back after every change.
            List<String> leases = spec.getExtensionData(LEASES, List.class);
            if (leases == null) {
                leases = new ArrayList<>();
            }
            leases.add(Long.toString(lease));
            spec.putExtensionData(LEASES, leases);
            return true;
        } catch (OperationFailureException e) {
            trigger.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            trigger.fail(Platform.inerr(e.getMessage()));
        }
        return false;
    }

    public static void releaseVm(VmInstanceSpec spec) {
        List<String> leases = spec.getExtensionData(LEASES, List.class);
        if (leases != null && !leases.isEmpty()) {
            SQL.New(JsonLabelVO.class).in(JsonLabelVO_.id,
                    leases.stream().map(Long::valueOf).collect(Collectors.toList())).hardDelete();
            spec.putExtensionData(LEASES, new ArrayList<String>());
        }
    }

    public static Completion releasingCompletion(VmInstanceSpec spec, Completion completion) {
        return new Completion(completion) {
            @Override
            public void success() {
                releaseVm(spec);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                releaseVm(spec);
                completion.fail(errorCode);
            }
        };
    }

    public void validateVmStorage(String hostUuid) {
        // A stopped VM has no hostUuid. A VM running elsewhere must not block
        // deletion of the host recorded only in its lastHostUuid.
        List<Tuple> vms = SQL.New("select vm.uuid, vm.name from VmInstanceVO vm" +
                        " where (vm.hostUuid = :hostUuid or" +
                        " (vm.hostUuid is null and vm.lastHostUuid = :hostUuid))" +
                        " and vm.state <> :destroyed order by vm.uuid", Tuple.class)
                .param("hostUuid", hostUuid).param("destroyed", VmInstanceState.Destroyed).list();
        if (vms.isEmpty()) {
            return;
        }

        Map<String, String> vmNames = new LinkedHashMap<>();
        vms.forEach(vm -> vmNames.put(vm.get(0, String.class), vm.get(1, String.class)));
        Map<String, String> volumeVms = new LinkedHashMap<>();
        List<Tuple> attached = SQL.New("select vol.uuid, vol.vmInstanceUuid from VolumeVO vol" +
                        " where vol.vmInstanceUuid in (:vmUuids) and vol.status <> :deleted order by vol.uuid", Tuple.class)
                .param("vmUuids", vmNames.keySet()).param("deleted", VolumeStatus.Deleted).list();
        attached.forEach(vol -> volumeVms.put(vol.get(0, String.class), vol.get(1, String.class)));
        // This extension resolves shareable-volume attachments by VM. Never
        // enumerate every shareable volume in the cloud to find this host's disks.
        for (String vmUuid : vmNames.keySet()) {
            for (BuildVolumeSpecExtensionPoint ext : pluginRgty.getExtensionList(BuildVolumeSpecExtensionPoint.class)) {
                for (VolumeInventory volume : ext.supplyAdditionalVolumesForVmInstance(vmUuid)) {
                    volumeVms.putIfAbsent(volume.getUuid(), vmUuid);
                }
            }
        }
        if (volumeVms.isEmpty()) {
            return;
        }

        List<Tuple> volumes = SQL.New("select vol, ps from VolumeVO vol, PrimaryStorageVO ps" +
                        " where vol.primaryStorageUuid = ps.uuid and ps.type <> :localStorageType" +
                        " and vol.status <> :deleted and vol.uuid in (:volumeUuids) order by vol.uuid", Tuple.class)
                .param("localStorageType", "LocalStorage").param("deleted", VolumeStatus.Deleted)
                .param("volumeUuids", volumeVms.keySet()).list();
        if (!volumes.isEmpty()) {
            Tuple tuple = volumes.get(0);
            VolumeVO volume = tuple.get(0, VolumeVO.class);
            PrimaryStorageVO storage = tuple.get(1, PrimaryStorageVO.class);
            String vmUuid = volumeVms.get(volume.getUuid());
            throw new OperationFailureException(operr(
                    "cannot delete host[uuid:%s]: vm[name:%s, uuid:%s] uses volume[name:%s, uuid:%s]" +
                            " on non-local primary storage[name:%s, uuid:%s, type:%s]." +
                            " Migrate the VM to another host or delete the VM before deleting this host",
                    hostUuid, vmNames.get(vmUuid), vmUuid, volume.getName(), volume.getUuid(),
                    storage.getName(), storage.getUuid(), storage.getType()));
        }
    }
}
