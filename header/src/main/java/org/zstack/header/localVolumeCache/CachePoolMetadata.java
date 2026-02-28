package org.zstack.header.localVolumeCache;

import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.util.List;

/**
 * Strongly-typed representation of the metadata JSON stored in VmLocalVolumeCachePoolVO.
 * <p>
 * VO stores metadata as a JSON String. VmLocalVolumeCachePoolInventory exposes it as a
 * LinkedHashMap (following the ExternalPrimaryStorage addonInfo pattern). This POJO
 * provides type-safe access for internal business logic.
 * <p>
 * Structure mirrors the Python agent's CachePoolMetadata:
 * <pre>
 * {
 *   "mountPoint": "/mnt/cache-pool-1",
 *   "pvs": [{"pvUuid": "...", "pvName": "...", "pvDevicePath": "/dev/nvme0n1"}],
 *   "vg": {"vgUuid": "...", "vgName": "..."},
 *   "lv": {"lvUuid": "...", "lvName": "...", "lvPath": "..."},
 *   "filesystem": {"fsUuid": "...", "fsType": "xfs"}
 * }
 * </pre>
 */
public class CachePoolMetadata implements Serializable {

    public static class PVRef implements Serializable {
        private String pvUuid;
        private String pvName;
        private String pvDevicePath;

        public String getPvUuid() {
            return pvUuid;
        }

        public void setPvUuid(String pvUuid) {
            this.pvUuid = pvUuid;
        }

        public String getPvName() {
            return pvName;
        }

        public void setPvName(String pvName) {
            this.pvName = pvName;
        }

        public String getPvDevicePath() {
            return pvDevicePath;
        }

        public void setPvDevicePath(String pvDevicePath) {
            this.pvDevicePath = pvDevicePath;
        }
    }

    public static class VGRef implements Serializable {
        private String vgUuid;
        private String vgName;

        public String getVgUuid() {
            return vgUuid;
        }

        public void setVgUuid(String vgUuid) {
            this.vgUuid = vgUuid;
        }

        public String getVgName() {
            return vgName;
        }

        public void setVgName(String vgName) {
            this.vgName = vgName;
        }
    }

    public static class LVRef implements Serializable {
        private String lvUuid;
        private String lvName;
        private String lvPath;

        public String getLvUuid() {
            return lvUuid;
        }

        public void setLvUuid(String lvUuid) {
            this.lvUuid = lvUuid;
        }

        public String getLvName() {
            return lvName;
        }

        public void setLvName(String lvName) {
            this.lvName = lvName;
        }

        public String getLvPath() {
            return lvPath;
        }

        public void setLvPath(String lvPath) {
            this.lvPath = lvPath;
        }
    }

    public static class FileSystemRef implements Serializable {
        private String fsUuid;
        private String fsType;

        public String getFsUuid() {
            return fsUuid;
        }

        public void setFsUuid(String fsUuid) {
            this.fsUuid = fsUuid;
        }

        public String getFsType() {
            return fsType;
        }

        public void setFsType(String fsType) {
            this.fsType = fsType;
        }
    }

    private String mountPoint;
    private List<PVRef> pvs;
    private VGRef vg;
    private LVRef lv;
    private FileSystemRef filesystem;

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public List<PVRef> getPvs() {
        return pvs;
    }

    public void setPvs(List<PVRef> pvs) {
        this.pvs = pvs;
    }

    public VGRef getVg() {
        return vg;
    }

    public void setVg(VGRef vg) {
        this.vg = vg;
    }

    public LVRef getLv() {
        return lv;
    }

    public void setLv(LVRef lv) {
        this.lv = lv;
    }

    public FileSystemRef getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(FileSystemRef filesystem) {
        this.filesystem = filesystem;
    }

    public static CachePoolMetadata fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new CachePoolMetadata();
        }
        return JSONObjectUtil.toObject(json, CachePoolMetadata.class);
    }

    public String toJson() {
        return JSONObjectUtil.toJsonString(this);
    }
}
