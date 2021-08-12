package org.zstack.zql;

public class ZQLStatistic {
    public ZQLStatistic(long cost) {
        this.minCost = cost;
        this.maxCost = cost;
    }

    private long minCost;
    private long maxCost;

    public long getMinCost() {
        return minCost;
    }

    public void setMinCost(long minCost) {
        this.minCost = minCost;
    }

    public long getMaxCost() {
        return maxCost;
    }

    public void setMaxCost(long maxCost) {
        this.maxCost = maxCost;
    }

    public void updateCostData(long cost) {
        this.minCost = Math.min(minCost, cost);
        this.maxCost = Math.max(maxCost, cost);
    }
}
