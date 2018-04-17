package org.zstack.storage.ceph;

import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
public interface CephCapacityUpdateExtensionPoint {
    void update(String fsid, long total, long avail, List<CephPoolCapacity> poolCapacities);
}
