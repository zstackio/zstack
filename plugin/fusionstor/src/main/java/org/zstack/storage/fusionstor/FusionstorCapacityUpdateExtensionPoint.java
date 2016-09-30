package org.zstack.storage.fusionstor;

/**
 * Created by frank on 7/28/2015.
 */
public interface FusionstorCapacityUpdateExtensionPoint {
    void update(String fsid, long total, long avail);
}
