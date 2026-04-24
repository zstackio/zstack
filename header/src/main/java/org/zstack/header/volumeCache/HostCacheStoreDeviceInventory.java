package org.zstack.header.volumeCache;

import java.io.Serializable;

/**
 * Per-device entry surfaced on a HostCacheStoreInventory.
 *
 * Only persisted fields are carried here: path and wwid. Live fields
 * (vendor/model/serial/size/present) are not enriched server-side; callers
 * that need live device info should call the block-devices API on the host
 * and join on wwid (preferred) or path (fallback).
 */
public class HostCacheStoreDeviceInventory implements Serializable {
    private String path;
    private String wwid;

    public static HostCacheStoreDeviceInventory valueOf(HostCacheStoreDeviceRef ref) {
        HostCacheStoreDeviceInventory inv = new HostCacheStoreDeviceInventory();
        inv.setPath(ref.getPath());
        inv.setWwid(ref.getWwid());
        return inv;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getWwid() { return wwid; }
    public void setWwid(String wwid) { this.wwid = wwid; }
}
