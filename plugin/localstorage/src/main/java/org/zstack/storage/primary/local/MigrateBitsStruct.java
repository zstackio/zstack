package org.zstack.storage.primary.local;

import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 11/16/2015.
 */
public class MigrateBitsStruct {
    public static class ResourceInfo {
        private LocalStorageResourceRefInventory resourceRef;
        private String path;

        public LocalStorageResourceRefInventory getResourceRef() {
            return resourceRef;
        }

        public void setResourceRef(LocalStorageResourceRefInventory resourceRef) {
            this.resourceRef = resourceRef;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
    private List<ResourceInfo> infos = new ArrayList<ResourceInfo>();
    private String srcHostUuid;
    private String destHostUuid;
    private String destPrimaryStorageUuid;
    private boolean crossPrimaryStorage = false;
    private String originBaseDir;
    private String newBaseDir;
    private String srcVolumeFolderPath;
    private String dstVolumeFolderPath;


    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public List<ResourceInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<ResourceInfo> infos) {
        this.infos = infos;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    public String getDestPrimaryStorageUuid() {
        return destPrimaryStorageUuid;
    }

    public void setDestPrimaryStorageUuid(String destPrimaryStorageUuid) {
        this.destPrimaryStorageUuid = destPrimaryStorageUuid;
    }

    public boolean CrossPrimaryStorage() {
        return crossPrimaryStorage;
    }

    public void setCrossPrimaryStorage(boolean crossPrimaryStorage) {
        this.crossPrimaryStorage = crossPrimaryStorage;
    }

    public String getOriginBaseDir() {
        return originBaseDir;
    }

    public void setOriginBaseDir(String originBaseDir) {
        this.originBaseDir = originBaseDir;
    }

    public String getNewBaseDir() {
        return newBaseDir;
    }

    public void setNewBaseDir(String newBaseDir) {
        this.newBaseDir = newBaseDir;
    }

    public String getSrcVolumeFolderPath() {
        return this.srcVolumeFolderPath;
    }

    public void setSrcVolumeFolderPath(String srcVolumeFolderPath) {
        this.srcVolumeFolderPath = srcVolumeFolderPath;
    }

    public String getDstVolumeFolderPath() {
        return this.dstVolumeFolderPath;
    }

    public void setDstVolumeFolderPath(String dstVolumeFolderPath) {
        this.dstVolumeFolderPath = dstVolumeFolderPath;
    }
}