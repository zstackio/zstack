package org.zstack.header.volumeCache;

import java.io.Serializable;
import java.util.Objects;

/**
 * Reference to a block device that belongs to a HostCacheStore.
 *
 * path: always populated — taken from user input or resolved live device entry.
 * wwid: optional — populated if the device has a stable WWID at the time of
 * pool creation/extension; null otherwise.
 *
 * Equality/hashing is based on (wwid, path) so that a ref with a known wwid
 * is only equal to another ref with the same wwid (regardless of path churn),
 * and a wwid-less ref is identified by path.
 */
public class HostCacheStoreDeviceRef implements Serializable {
    private String path;
    private String wwid;

    public HostCacheStoreDeviceRef() {
    }

    public HostCacheStoreDeviceRef(String path, String wwid) {
        this.path = path;
        this.wwid = wwid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostCacheStoreDeviceRef)) return false;
        HostCacheStoreDeviceRef that = (HostCacheStoreDeviceRef) o;
        if (wwid != null && that.wwid != null) {
            return wwid.equals(that.wwid);
        }
        if (wwid != null || that.wwid != null) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return wwid != null ? wwid.hashCode() : Objects.hashCode(path);
    }
}
