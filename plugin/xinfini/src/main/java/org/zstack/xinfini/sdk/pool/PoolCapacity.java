package org.zstack.xinfini.sdk.pool;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:26 2024/5/29
 */
public class PoolCapacity {
    private long availableCapacity;
    private long totalCapacity;

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
