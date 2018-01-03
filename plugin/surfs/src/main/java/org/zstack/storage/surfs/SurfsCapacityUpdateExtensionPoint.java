package org.zstack.storage.surfs;

/**
 * Created by zhouhaiping 2017-09-01
 */
public interface SurfsCapacityUpdateExtensionPoint {
    void update(String fsid,long total, long avail);
}