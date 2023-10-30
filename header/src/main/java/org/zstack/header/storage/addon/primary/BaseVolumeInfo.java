package org.zstack.header.storage.addon.primary;

import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeStats;

import java.util.Collection;
import java.util.List;

public class BaseVolumeInfo extends VolumeStats {
    // optional, zsUUid
    String uuid;
    // optional
    VolumeQos qos;

    protected String primaryStorageUuid;
    protected VolumeProtocol protocol;
    protected boolean shareable;

    public BaseVolumeInfo(VolumeStats stats) {
        super(stats.getInstallPath(), stats.getActualSize());
    }

    public BaseVolumeInfo() {
        super();
    }

    public void setProtocol(VolumeProtocol protocol) {
        this.protocol = protocol;
    }

    public VolumeProtocol getProtocol() {
        return protocol;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    public boolean isShareable() {
        return shareable;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public static BaseVolumeInfo valueOf(VolumeInventory vol) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = vol.getUuid();
        info.setActualSize(vol.getActualSize());
        info.setInstallPath(vol.getInstallPath());
        if (vol.getProtocol() != null) {
            info.setProtocol(VolumeProtocol.valueOf(vol.getProtocol()));
        }
        info.setQos(VolumeQos.valueOf(vol.getVolumeQos()));
        info.setShareable(vol.isShareable());
        info.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
        return info;
    }

    public static List<BaseVolumeInfo> valueOf(Collection<VolumeInventory> vols) {
        return vols.stream().map(BaseVolumeInfo::valueOf).collect(java.util.stream.Collectors.toList());
    }

}
