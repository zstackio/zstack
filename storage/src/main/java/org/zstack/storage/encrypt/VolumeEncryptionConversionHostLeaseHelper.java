package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.getManagementServerId;
import static org.zstack.core.Platform.operr;

public class VolumeEncryptionConversionHostLeaseHelper {
    private static final CLogger logger = Utils.getLogger(VolumeEncryptionConversionHostLeaseHelper.class);
    private static final String LEASE_KEY_PREFIX = "volumeEncryptionConversionHostLease::";
    private static final String LEASE_LOCK_PREFIX = "volEncConvHost::";
    private static final String LEASE_SEPARATOR = "::";
    private static final String LOCAL_STORAGE_HOST_TAG_PREFIX = "localStorage::hostUuid::";
    public static final long WAIT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3);
    public static final long WAIT_TIMEOUT_MS = TimeUnit.HOURS.toMillis(2);
    public static final long LEASE_TTL_MS = TimeUnit.MINUTES.toMillis(30);
    public static final long LEASE_RENEW_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    public static class Lease {
        private String hostUuid;
        private String vmUuid;
        private String volumeUuid;
        private String labelValue;
        private boolean released;

        public String getHostUuid() {
            return hostUuid;
        }

        public boolean isReleased() {
            return released;
        }
    }

    public static class LeaseSession {
        private Lease lease;
        private ThreadFacadeImpl.TimeoutTaskReceipt renewReceipt;

        public String getHostUuid() {
            return lease == null ? null : lease.getHostUuid();
        }
    }

    public static class Reservation {
        private Lease lease;
        private String hostUuid;
        private String vmUuid;
        private boolean sameVmBusy;

        public Lease getLease() {
            return lease;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public boolean isSameVmBusy() {
            return sameVmBusy;
        }
    }

    public String makeQueueSignature(String volumeUuid, String fallback) {
        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (volume == null) {
            return fallback;
        }

        try {
            String hostUuid = resolveHostUuid(volume);
            if (StringUtils.isBlank(hostUuid)) {
                return fallback;
            }

            return String.format("volumeEncryptionConversion::%s::%s", hostUuid, resolveVmUuid(volume));
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to make volume encryption conversion queue signature for volume[uuid:%s]: %s",
                    volumeUuid, e.getMessage()), e);
            return fallback;
        }
    }

    public Reservation tryReserve(VolumeVO volume) {
        String hostUuid = resolveHostUuid(volume);
        if (StringUtils.isBlank(hostUuid)) {
            throw new OperationFailureException(operr(
                    "cannot find a connected KVM host to convert volume[uuid:%s] encryption on primary storage[uuid:%s]",
                    volume.getUuid(), volume.getPrimaryStorageUuid()));
        }

        String vmUuid = resolveVmUuid(volume);
        GLock lock = newLeaseLock(hostUuid);
        try {
            String labelKey = leaseKey(hostUuid);
            JsonLabelVO label = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, labelKey).find();
            if (label == null) {
                return reserved(createLease(labelKey, hostUuid, vmUuid, volume.getUuid()), hostUuid, vmUuid);
            }

            if (leaseExpired(label) || ownerManagementNodeRestarted(label)) {
                SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, labelKey).hardDelete();
                return reserved(createLease(labelKey, hostUuid, vmUuid, volume.getUuid()), hostUuid, vmUuid);
            }

            String ownerVmUuid = leaseVmUuid(label.getLabelValue());
            if (StringUtils.isBlank(ownerVmUuid)) {
                SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, labelKey).hardDelete();
                return reserved(createLease(labelKey, hostUuid, vmUuid, volume.getUuid()), hostUuid, vmUuid);
            }

            if (!vmUuid.equals(ownerVmUuid)) {
                throw new OperationFailureException(operr(
                        "host[uuid:%s] is converting volume encryption for vm[uuid:%s], cannot convert volume[uuid:%s] for vm[uuid:%s] at the same time",
                        hostUuid, ownerVmUuid, volume.getUuid(), vmUuid));
            }

            return sameVmBusy(hostUuid, vmUuid);
        } finally {
            lock.unlock();
        }
    }

    public void reserve(VolumeVO volume, FlowTrigger trigger, AtomicReference<LeaseSession> sessionRef) {
        reserve(volume, trigger, sessionRef, System.currentTimeMillis() + WAIT_TIMEOUT_MS);
    }

    public void release(LeaseSession session) {
        if (session == null) {
            return;
        }

        if (session.renewReceipt != null) {
            session.renewReceipt.cancel();
        }
        release(session.lease);
    }

    public boolean renew(Lease lease) {
        if (lease == null || lease.released) {
            return false;
        }

        GLock lock;
        try {
            lock = newLeaseLock(lease.hostUuid);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to lock host[uuid:%s] lease while renewing volume[uuid:%s] encryption conversion: %s",
                    lease.hostUuid, lease.volumeUuid, e.getMessage()), e);
            return false;
        }

        try {
            JsonLabelVO label = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, leaseKey(lease.hostUuid)).find();
            if (label == null || !lease.labelValue.equals(label.getLabelValue())) {
                return false;
            }

            lease.labelValue = makeLeaseValue(lease.vmUuid, lease.volumeUuid, newLeaseExpireAt(), currentManagementNodeJoinDate());
            label.setLabelValue(lease.labelValue);
            dbf.update(label);
            return true;
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to renew host[uuid:%s] lease for volume[uuid:%s] encryption conversion: %s",
                    lease.hostUuid, lease.volumeUuid, e.getMessage()), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void reserve(VolumeVO volume, FlowTrigger trigger, AtomicReference<LeaseSession> sessionRef, long deadline) {
        try {
            Reservation reservation = tryReserve(volume);
            if (reservation.getLease() != null) {
                LeaseSession session = new LeaseSession();
                session.lease = reservation.getLease();
                sessionRef.set(session);
                scheduleRenew(session);
                trigger.next();
                return;
            }

            if (!reservation.isSameVmBusy()) {
                trigger.fail(operr("failed to reserve host for converting volume[uuid:%s] encryption", volume.getUuid()));
                return;
            }

            if (System.currentTimeMillis() >= deadline) {
                trigger.fail(operr(
                        "timeout waiting for vm[uuid:%s] volume encryption conversion on host[uuid:%s] before converting volume[uuid:%s]",
                        reservation.getVmUuid(), reservation.getHostUuid(), volume.getUuid()));
                return;
            }

            thdf.submitTimeoutTask(() -> reserve(volume, trigger, sessionRef, deadline),
                    TimeUnit.MILLISECONDS, WAIT_INTERVAL_MS);
        } catch (OperationFailureException e) {
            trigger.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            trigger.fail(operr("failed to reserve host for converting volume[uuid:%s] encryption: %s",
                    volume.getUuid(), e.getMessage()));
        }
    }

    private void scheduleRenew(LeaseSession session) {
        if (session == null || session.lease == null || session.lease.isReleased()) {
            return;
        }

        session.renewReceipt = thdf.submitTimeoutTask(() -> {
            if (renew(session.lease)) {
                scheduleRenew(session);
            }
        }, TimeUnit.MILLISECONDS, LEASE_RENEW_INTERVAL_MS);
    }

    public void release(Lease lease) {
        if (lease == null) {
            return;
        }
        if (lease.released) {
            return;
        }
        lease.released = true;

        GLock lock;
        try {
            lock = newLeaseLock(lease.hostUuid);
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to lock host[uuid:%s] lease while releasing volume[uuid:%s] encryption conversion: %s",
                    lease.hostUuid, lease.volumeUuid, e.getMessage()), e);
            return;
        }

        try {
            String labelKey = leaseKey(lease.hostUuid);
            JsonLabelVO label = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, labelKey).find();
            if (label == null) {
                return;
            }

            if (!lease.labelValue.equals(label.getLabelValue())) {
                return;
            }

            SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, labelKey).hardDelete();
        } catch (RuntimeException e) {
            logger.warn(String.format("failed to release host[uuid:%s] lease for volume[uuid:%s] encryption conversion: %s",
                    lease.hostUuid, lease.volumeUuid, e.getMessage()), e);
        } finally {
            lock.unlock();
        }
    }

    private Lease createLease(String labelKey, String hostUuid, String vmUuid, String volumeUuid) {
        String labelValue = makeLeaseValue(vmUuid, volumeUuid, newLeaseExpireAt(), currentManagementNodeJoinDate());
        JsonLabelVO label = new JsonLabelVO();
        label.setLabelKey(labelKey);
        label.setResourceUuid(hostUuid);
        label.setLabelValue(labelValue);
        dbf.persist(label);

        Lease lease = new Lease();
        lease.hostUuid = hostUuid;
        lease.vmUuid = vmUuid;
        lease.volumeUuid = volumeUuid;
        lease.labelValue = labelValue;
        return lease;
    }

    private Reservation reserved(Lease lease, String hostUuid, String vmUuid) {
        Reservation reservation = new Reservation();
        reservation.lease = lease;
        reservation.hostUuid = hostUuid;
        reservation.vmUuid = vmUuid;
        return reservation;
    }

    private Reservation sameVmBusy(String hostUuid, String vmUuid) {
        Reservation reservation = new Reservation();
        reservation.hostUuid = hostUuid;
        reservation.vmUuid = vmUuid;
        reservation.sameVmBusy = true;
        return reservation;
    }

    private GLock newLeaseLock(String hostUuid) {
        GLock lock = new GLock(LEASE_LOCK_PREFIX + hostUuid, TimeUnit.SECONDS.toSeconds(30));
        lock.lock();
        return lock;
    }

    private String leaseKey(String hostUuid) {
        return LEASE_KEY_PREFIX + hostUuid;
    }

    private String leaseVmUuid(String labelValue) {
        return StringUtils.substringBefore(labelValue, LEASE_SEPARATOR);
    }

    private boolean ownerManagementNodeRestarted(JsonLabelVO label) {
        String ownerUuid = leaseOwnerManagementNodeUuid(label.getLabelValue());
        Long ownerJoinDate = leaseOwnerManagementNodeJoinDate(label.getLabelValue());
        if (StringUtils.isBlank(ownerUuid) || ownerJoinDate == null) {
            return false;
        }

        Date currentOwnerJoinDate = Q.New(ManagementNodeVO.class)
                .eq(ManagementNodeVO_.uuid, ownerUuid)
                .select(ManagementNodeVO_.joinDate)
                .findValue();
        return currentOwnerJoinDate == null || !ownerJoinDate.equals(currentOwnerJoinDate.getTime());
    }

    private boolean leaseExpired(JsonLabelVO label) {
        Long expireAt = leaseExpireAt(label.getLabelValue());
        if (expireAt != null) {
            return expireAt < System.currentTimeMillis();
        }

        Date lastAliveDate = label.getLastOpDate() == null ? label.getCreateDate() : label.getLastOpDate();
        return lastAliveDate == null || System.currentTimeMillis() - lastAliveDate.getTime() > LEASE_TTL_MS;
    }

    private Long leaseExpireAt(String labelValue) {
        return leaseLongToken(labelValue, 2);
    }

    private Long leaseOwnerManagementNodeJoinDate(String labelValue) {
        return leaseLongToken(labelValue, 4);
    }

    private String leaseOwnerManagementNodeUuid(String labelValue) {
        return leaseToken(labelValue, 3);
    }

    private Long leaseLongToken(String labelValue, int index) {
        String value = leaseToken(labelValue, index);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String leaseToken(String labelValue, int index) {
        if (StringUtils.isBlank(labelValue)) {
            return null;
        }

        int start = 0;
        for (int i = 0; i < index; i++) {
            int separator = labelValue.indexOf(LEASE_SEPARATOR, start);
            if (separator < 0) {
                return null;
            }
            start = separator + LEASE_SEPARATOR.length();
        }

        int end = labelValue.indexOf(LEASE_SEPARATOR, start);
        return end < 0 ? labelValue.substring(start) : labelValue.substring(start, end);
    }

    private long newLeaseExpireAt() {
        return System.currentTimeMillis() + LEASE_TTL_MS;
    }

    private String makeLeaseValue(String vmUuid, String volumeUuid, long expireAt, Long ownerManagementNodeJoinDate) {
        return vmUuid + LEASE_SEPARATOR + volumeUuid + LEASE_SEPARATOR + expireAt +
                LEASE_SEPARATOR + getManagementServerId() +
                LEASE_SEPARATOR + (ownerManagementNodeJoinDate == null ? "" : ownerManagementNodeJoinDate);
    }

    private Long currentManagementNodeJoinDate() {
        Date joinDate = Q.New(ManagementNodeVO.class)
                .eq(ManagementNodeVO_.uuid, getManagementServerId())
                .select(ManagementNodeVO_.joinDate)
                .findValue();
        return joinDate == null ? null : joinDate.getTime();
    }

    private String resolveVmUuid(VolumeVO volume) {
        String vmUuid = StringUtils.defaultIfBlank(volume.getVmInstanceUuid(), volume.getLastVmInstanceUuid());
        return StringUtils.defaultIfBlank(vmUuid, volume.getUuid());
    }

    private String resolveHostUuid(VolumeVO volume) {
        String hostUuid = resolveVolumeSecretHostUuid(volume);
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        hostUuid = resolveVmHostUuid(volume.getVmInstanceUuid());
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        hostUuid = resolveVmHostUuid(volume.getLastVmInstanceUuid());
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        hostUuid = resolveLocalStorageHostUuidFromVolumeTag(volume);
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        hostUuid = resolveLocalStorageHostUuidFromResourceRef(volume);
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        if (isLocalStoragePrimaryStorage(volume.getPrimaryStorageUuid())) {
            return null;
        }

        return findConnectedKvmHostAttachedToPrimaryStorage(volume.getPrimaryStorageUuid());
    }

    private String resolveVolumeSecretHostUuid(VolumeVO volume) {
        if (volume == null) {
            return null;
        }

        List<String> tags = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTags(volume.getUuid(), VolumeVO.class);
        if (tags != null && !tags.isEmpty()) {
            return VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTokenByTag(
                    tags.get(0), VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN);
        }

        return resolveVmHostUuid(volume.getVmInstanceUuid());
    }

    private String resolveVmHostUuid(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        if (vm == null) {
            return null;
        }

        return StringUtils.defaultIfBlank(vm.getHostUuid(), vm.getLastHostUuid());
    }

    private String resolveLocalStorageHostUuidFromVolumeTag(VolumeVO volume) {
        List<String> tags = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, volume.getUuid())
                .like(SystemTagVO_.tag, LOCAL_STORAGE_HOST_TAG_PREFIX + "%")
                .select(SystemTagVO_.tag)
                .listValues();
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        return StringUtils.substringAfter(tags.get(0), LOCAL_STORAGE_HOST_TAG_PREFIX);
    }

    private String resolveLocalStorageHostUuidFromResourceRef(VolumeVO volume) {
        if (!isLocalStoragePrimaryStorage(volume.getPrimaryStorageUuid())) {
            return null;
        }

        return SQL.New("select hostUuid from LocalStorageResourceRefVO where resourceUuid = :resourceUuid and primaryStorageUuid = :primaryStorageUuid and resourceType = :resourceType", String.class)
                .param("resourceUuid", volume.getUuid())
                .param("primaryStorageUuid", volume.getPrimaryStorageUuid())
                .param("resourceType", VolumeVO.class.getSimpleName())
                .limit(1)
                .find();
    }

    private boolean isLocalStoragePrimaryStorage(String primaryStorageUuid) {
        String primaryStorageType = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageVO_.type)
                .findValue();
        return "LocalStorage".equals(primaryStorageType);
    }

    private String findConnectedKvmHostAttachedToPrimaryStorage(String primaryStorageUuid) {
        List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageUuid)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();
        if (clusterUuids.isEmpty()) {
            return null;
        }

        List<String> connectedHostUuids = Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                .select(PrimaryStorageHostRefVO_.hostUuid)
                .listValues();
        if (!Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .isExists()) {
            connectedHostUuids = Collections.emptyList();
        } else if (connectedHostUuids.isEmpty()) {
            return null;
        }

        SimpleQuery<HostVO> hostQuery = dbf.createQuery(HostVO.class);
        hostQuery.add(HostVO_.clusterUuid, SimpleQuery.Op.IN, clusterUuids);
        hostQuery.add(HostVO_.hypervisorType, SimpleQuery.Op.EQ, VmInstanceConstant.KVM_HYPERVISOR_TYPE);
        hostQuery.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected);
        hostQuery.add(HostVO_.state, SimpleQuery.Op.EQ, HostState.Enabled);
        if (!connectedHostUuids.isEmpty()) {
            hostQuery.add(HostVO_.uuid, SimpleQuery.Op.IN, connectedHostUuids);
        }
        hostQuery.select(HostVO_.uuid);
        hostQuery.orderBy(HostVO_.uuid, SimpleQuery.Od.ASC);
        hostQuery.setLimit(1);
        return hostQuery.findValue();
    }
}
