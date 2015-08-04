package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.storage.ceph.CephConstants;

import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
public class APIAddCephPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam(nonempty = false)
    private List<String> monUrls;
    @APIParam(required = false, maxLength = 255)
    private String rootVolumePoolName;
    @APIParam(required = false, maxLength = 255)
    private String dataVolumePoolName;
    @APIParam(required = false, maxLength = 255)
    private String imageCachePoolName;
    @APIParam(required = false, maxLength = 255)
    private String snapshotPoolName;

    public String getRootVolumePoolName() {
        return rootVolumePoolName;
    }

    public void setRootVolumePoolName(String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }

    public String getDataVolumePoolName() {
        return dataVolumePoolName;
    }

    public void setDataVolumePoolName(String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }

    public String getImageCachePoolName() {
        return imageCachePoolName;
    }

    public void setImageCachePoolName(String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }

    public String getSnapshotPoolName() {
        return snapshotPoolName;
    }

    public void setSnapshotPoolName(String snapshotPoolName) {
        this.snapshotPoolName = snapshotPoolName;
    }

    @Override
    public String getType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }
}
