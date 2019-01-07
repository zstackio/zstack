package org.zstack.kvm;

import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;
import java.util.stream.Collectors;

public class VolumeTO {
    public static final String FILE = "file";
    public static final String ISCSI = "iscsi";
    public static final String CEPH = "ceph";
    public static final String FUSIONSTOR = "fusionstor";
    public static final String SHAREDBLOCK = "sharedblock";
    public static final String SCSILUN = "scsilun";
    public static final String BLOCK = "block";

    private String installPath;
    private int deviceId;
    private String deviceType = FILE;
    private String volumeUuid;
    private boolean useVirtio;
    private boolean useVirtioSCSI;
    private boolean shareable;
    private String cacheMode = "none";
    private String wwn;
    public VolumeTO() {
    }

    public VolumeTO(VolumeTO other) {
        this.installPath = other.installPath;
        this.deviceId = other.deviceId;
        this.deviceType = other.deviceType;
        this.volumeUuid = other.volumeUuid;
        this.useVirtio = other.useVirtio;
        this.useVirtioSCSI = other.useVirtioSCSI;
        this.cacheMode = other.cacheMode;
        this.wwn = other.wwn;
        this.shareable = other.shareable;
    }

    public static List<VolumeTO> valueOf(List<VolumeInventory> vols) {
        return vols.stream().map(VolumeTO::valueOf).collect(Collectors.toList());
    }

    public static VolumeTO valueOf(VolumeInventory vol) {
        return valueOf(vol, null);
    }

    public static VolumeTO valueOf(VolumeInventory vol, String platform) {
        VolumeTO to = new VolumeTO();
        to.setInstallPath(vol.getInstallPath());
        if (vol.getDeviceId() != null) {
            to.setDeviceId(vol.getDeviceId());
        }
        to.setDeviceType(KVMHost.getVolumeTOType(vol));
        to.setVolumeUuid(vol.getUuid());
        // volumes can only be attached on Windows if the virtio is enabled
        // so for Windows, use virtio as well

        if (platform == null && vol.getVmInstanceUuid() != null) {
            platform = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vol.getUuid()).select(VmInstanceVO_.platform).findValue();
        }

        to.setUseVirtio(platform == null || (ImagePlatform.Windows.toString().equals(platform) ||
                ImagePlatform.valueOf(platform).isParaVirtualization()));
        to.setUseVirtioSCSI(KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(vol.getUuid()));
        to.setWwn(KVMHost.computeWwnIfAbsent(vol.getUuid()));
        to.setShareable(vol.isShareable());
        to.setCacheMode(KVMGlobalConfig.LIBVIRT_CACHE_MODE.value());
        return to;
    }

    public boolean isShareable() {
        return shareable;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public boolean isUseVirtioSCSI() {
        return useVirtioSCSI;
    }

    public void setUseVirtioSCSI(boolean useVirtioSCSI) {
        this.useVirtioSCSI = useVirtioSCSI;
    }

    public boolean isUseVirtio() {
        return useVirtio;
    }

    public void setUseVirtio(boolean useVirtio) {
        this.useVirtio = useVirtio;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }
}
