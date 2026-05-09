package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Volume expunge cleanup for LUKS-encrypted volumes.
 *
 * <p>Mirrors the {@code VmJustBeforeDeleteFromDb} / {@code VmAfterExpunge}
 * pattern used by vTPM in {@code KvmTpmExtensions}, but at the volume layer:
 *
 * <ul>
 *   <li><b>Trigger</b>: {@link VolumeJustBeforeDeleteFromDbExtensionPoint#volumeJustBeforeDeleteFromDb}
 *       — runs at the tail of {@code VolumeBase} expunge, after the agent
 *       has destroyed the on-disk bits, just before the {@code VolumeVO}
 *       row is removed.</li>
 *   <li><b>Not triggered on</b>: VM destroy with {@code Delay} policy (volume
 *       lives on, parked in recycle bin); VM destroy with {@code KeepVolume}
 *       (data volume preserved); volume detach without delete. In all these
 *       cases the encryption metadata must stay so the volume can be
 *       attached / restored later.</li>
 * </ul>
 *
 * <p>Cleanup actions, in order:
 * <ol>
 *   <li>Look up {@code keyVersion} + host while the binding is still in DB.</li>
 *   <li>Best-effort delete the libvirt secret on the host (if we can locate
 *       one — bound only when the volume is attached to a VM whose
 *       host/lastHost is known).</li>
 *   <li>Delete the {@code EncryptedResourceKeyRefVO} row — this is the
 *       persistent piece, and the one that absolutely must be cleared so
 *       a future volume reusing the same uuid doesn't inherit a stale
 *       binding.</li>
 * </ol>
 *
 * <p>Order matters: keyVersion lives on the ref row, so we must read it
 * before {@code detachKeyProviderFromVolume} wipes the row.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedExpungeExtension implements VolumeJustBeforeDeleteFromDbExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VolumeEncryptedExpungeExtension.class);

    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper secretHelper;

    @Override
    public void volumeJustBeforeDeleteFromDb(VolumeInventory inv) {
        if (inv == null || !Boolean.TRUE.equals(inv.getEncrypted())) {
            return;
        }
        String volUuid = inv.getUuid();

        // Snapshot keyVersion BEFORE detaching the binding, otherwise the
        // host-side cleanup loses the key it needs to identify the secret.
        Integer keyVersion = volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volUuid);

        String hostUuid = resolveHostUuidFromTag(volUuid);
        if (StringUtils.isBlank(hostUuid)) {
            String vmUuid = StringUtils.defaultIfBlank(inv.getVmInstanceUuid(), inv.getLastVmInstanceUuid());
            hostUuid = resolveHostUuidFromVm(vmUuid);
        }

        // vmUuid passed to the key-agent's DeleteSecret RPC is a contractual
        // leftover (the per-volume secret usage name doesn't actually need it
        // post Method-D); pass whatever we can reconstruct, or fall back to
        // the volume uuid as a stable placeholder so validation passes.
        String vmUuidForRpc = StringUtils.defaultIfBlank(
                StringUtils.defaultIfBlank(inv.getVmInstanceUuid(), inv.getLastVmInstanceUuid()),
                volUuid);

        if (StringUtils.isNotBlank(hostUuid) && keyVersion != null) {
            try {
                secretHelper.deleteSecretOnHostBestEffort(hostUuid, vmUuidForRpc, volUuid, keyVersion);
            } catch (RuntimeException e) {
                // helper is best-effort, but guard against unchecked throws
                // so we still get to the DB cleanup below.
                logger.warn(String.format(
                        "ignoring failure to delete libvirt LUKS secret for volume[uuid:%s] on host[uuid:%s]: %s",
                        volUuid, hostUuid, e.getMessage()));
            }
        } else {
            logger.debug(String.format(
                    "skip host-side libvirt secret cleanup for volume[uuid:%s]:" +
                            " hostUuid=%s keyVersion=%s",
                    volUuid, hostUuid, keyVersion));
        }

        try {
            volumeEncryptedResourceKeyBackend.detachKeyProviderFromVolume(volUuid);
        } catch (RuntimeException e) {
            logger.warn(String.format(
                    "failed to detach EncryptedResourceKeyRefVO for volume[uuid:%s] on expunge: %s",
                    volUuid, e.getMessage()));
        }
    }

    /**
     * Primary host lookup: read the {@code VOLUME_LIBVIRT_SECRET_HOST} systemtag
     * that {@code VolumeEncryptedSecretHelper.defineLibvirtSecretOnHost} stamps
     * after every successful libvirt secret define. The tag is non-inherent and
     * lives on the {@code VolumeVO} row, so it survives anything short of the
     * volume itself being deleted.
     */
    private String resolveHostUuidFromTag(String volUuid) {
        List<String> tags = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTags(volUuid, VolumeVO.class);
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTokenByTag(
                tags.get(0), VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN);
    }

    /**
     * Fallback host lookup for volumes created before the
     * {@code VOLUME_LIBVIRT_SECRET_HOST} tag mechanism existed. Walks the
     * vmInstanceUuid / lastVmInstanceUuid → VmInstanceVO.hostUuid /
     * lastHostUuid chain.
     */
    private String resolveHostUuidFromVm(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }
        String host = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (StringUtils.isNotBlank(host)) {
            return host;
        }
        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid)
                .findValue();
    }
}
