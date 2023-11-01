package org.zstack.header.storage.addon.primary;

import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeQos;
import org.zstack.header.volume.VolumeStats;

import java.util.Collection;
import java.util.List;

public class BaseVolumeInfo extends VolumeStats {
    // optional, zsUUid
    String uuid;
    // optional
    VolumeQos qos;

    protected String primaryStorageUuid;
    protected String protocol;
    protected boolean shareable;

    public BaseVolumeInfo(VolumeStats stats) {
        super(stats.getInstallPath(), stats.getActualSize());
    }

    public BaseVolumeInfo() {
        super();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
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

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public static BaseVolumeInfo valueOf(VolumeInventory vol) {
        BaseVolumeInfo info = new BaseVolumeInfo();
        info.uuid = vol.getUuid();
        info.setActualSize(vol.getActualSize());
        info.setInstallPath(vol.getInstallPath());
        info.setProtocol(vol.getProtocol());
        info.setQos(VolumeQos.valueOf(vol.getVolumeQos()));
        info.setShareable(vol.isShareable());
        info.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
        return info;
    }

    public static List<BaseVolumeInfo> valueOf(Collection<VolumeInventory> vols) {
        return vols.stream().map(BaseVolumeInfo::valueOf).collect(java.util.stream.Collectors.toList());
    }

}
