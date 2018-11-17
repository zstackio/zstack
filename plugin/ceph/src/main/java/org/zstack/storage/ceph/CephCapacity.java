package org.zstack.storage.ceph;

import java.util.List;

/**
 * Created by lining on 2018/11/7.
 */
public class CephCapacity {
    private String fsid;
    private Long totalCapacity;
    private Long availableCapacity;
    private List<CephPoolCapacity> poolCapacities;
    private boolean xsky = false;

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public List<CephPoolCapacity> getPoolCapacities() {
        return poolCapacities;
    }

    public void setPoolCapacities(List<CephPoolCapacity> poolCapacities) {
        this.poolCapacities = poolCapacities;
    }

    public boolean isXsky() {
        return xsky;
    }

    public void setXsky(boolean xsky) {
        this.xsky = xsky;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
}
