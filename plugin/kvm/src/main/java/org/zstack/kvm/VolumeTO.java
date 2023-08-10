package org.zstack.kvm;

import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;
import java.util.stream.Collectors;

public class VolumeTO extends BaseVirtualDeviceTO {
    public static final String FILE = "file";
    public static final String ISCSI = "iscsi";
    public static final String CEPH = "ceph";
    public static final String SHAREDBLOCK = "sharedblock";
    public static final String SCSILUN = "scsilun";
    public static final String BLOCK = "block";
    public static final String MINISTORAGE = "mini";
    public static final String QUORUM = "quorum";
    public static List<KVMConvertVolumeExtensionPoint> exts;

    private String installPath;
    private int deviceId;
    private String deviceType = FILE;
    /**
     * if this volume is lun, use lunUuid
     */
    private String volumeUuid;
    private boolean useVirtio;
    private boolean useVirtioSCSI;
    private boolean shareable;
    private String cacheMode = "none";
    private String wwn;
    private int bootOrder;
    private int physicalBlockSize;

    // for baremetal2 instance
    private String type;
    private String format;
    private String primaryStorageType;
    private String multiQueues;
    private int ioThreadId;
    private String ioThreadPin;
    private int controllerIndex;

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
        this.bootOrder = other.bootOrder;
        this.physicalBlockSize = other.physicalBlockSize;
        this.type = other.type;
        this.format = other.format;
        this.primaryStorageType = other.primaryStorageType;
        this.multiQueues = other.multiQueues;
        this.ioThreadId = other.ioThreadId;
        this.ioThreadPin = other.ioThreadPin;
        this.controllerIndex = other.controllerIndex;
    }

    public static List<VolumeTO> valueOf(List<VolumeInventory> vols, KVMHostInventory host) {
        return vols.stream().map(it -> valueOf(it, host)).collect(Collectors.toList());
    }

    public static VolumeTO valueOf(VolumeInventory vol, KVMHostInventory host) {
        return valueOf(vol, host, null, true);
    }

    public static VolumeTO valueOfWithOutExtension(VolumeInventory vol, KVMHostInventory host, String platform) {
        return valueOf(vol, host, platform, false);
    }

    public static VolumeTO valueOf(VolumeInventory vol, KVMHostInventory host, String platform, boolean withExtension) {
        VolumeTO to = new VolumeTO();
        to.setResourceUuid(vol.getUuid());
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

        boolean diskVolume = VolumeConstant.VOLUME_FORMAT_DISK.equals(vol.getFormat());
        to.setUseVirtio(!diskVolume && (platform == null || (ImagePlatform.Windows.toString().equals(platform) ||
                ImagePlatform.valueOf(platform).isParaVirtualization())));
        to.setUseVirtioSCSI(!ImagePlatform.Other.toString().equals(platform) && KVMSystemTags.VOLUME_VIRTIO_SCSI.hasTag(vol.getUuid()));
        to.setWwn(KVMHost.computeWwnIfAbsent(vol.getUuid()));
        to.setShareable(vol.isShareable());
        to.setCacheMode(KVMGlobalConfig.LIBVIRT_CACHE_MODE.value());

        String psType = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, vol.getPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type)
                .findValue();
        to.setType(vol.getType());
        to.setFormat(vol.getFormat());
        to.setPrimaryStorageType(psType);

        if (!withExtension) {
            return to;
        }

        if (exts == null) {
            prepareExts();
        }
        for (KVMConvertVolumeExtensionPoint ext : exts) {
            to = ext.convertVolumeIfNeed(host, vol, to);
        }   
        return  to;
    }

    private synchronized static void prepareExts() {
        if (exts == null) {
            exts = Platform.getComponentLoader().getComponent(PluginRegistry.class).getExtensionList(KVMConvertVolumeExtensionPoint.class);
        }
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

    public int getBootOrder() {
        return bootOrder;
    }

    public void setBootOrder(int bootOrder) {
        this.bootOrder = bootOrder;
    }

    public int getPhysicalBlockSize() {
        return physicalBlockSize;
    }

    public void setPhysicalBlockSize(int physicalBlockSize) {
        this.physicalBlockSize = physicalBlockSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPrimaryStorageType() {
        return primaryStorageType;
    }

    public void setPrimaryStorageType(String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }

    public void setMultiQueues(String multiQueues) {
        this.multiQueues = multiQueues;
    }

    public String getMultiQueues() {
        return multiQueues;
    }

    public void setIoThreadId(int ioThreadId) {
        this.ioThreadId = ioThreadId;
    }

    public int getIoThreadId() {
        return ioThreadId;
    }

    public void setIoThreadPin(String ioThreadPin) {
        this.ioThreadPin = ioThreadPin;
    }

    public String getIoThreadPin() {
        return ioThreadPin;
    }

    public int getControllerIndex() {
        return controllerIndex;
    }

    public void setControllerIndex(int controllerIndex) {
        this.controllerIndex = controllerIndex;
    }
}
