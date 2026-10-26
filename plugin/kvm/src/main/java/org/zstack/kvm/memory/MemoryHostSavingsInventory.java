package org.zstack.kvm.memory;

/** One requested Host in GetMemorySummary, projected exactly as GetMemoryStates does. */
public class MemoryHostSavingsInventory {
    public static MemoryHostSavingsInventory __example__() {
        MemoryHostSavingsInventory inventory = new MemoryHostSavingsInventory();
        inventory.setHostUuid("1234567890abcdef1234567890abcdef");
        inventory.setMetrics(java.util.Collections.<String, Object>singletonMap("totalSavedEstimateBytes", 1610612736L));
        inventory.setQuality("Fresh");
        inventory.setSampleTime(1791583200000L);
        return inventory;
    }

    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    private java.util.Map<String, Object> metrics;
    public java.util.Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(java.util.Map<String, Object> value) { metrics = value; }

    private String quality;
    public String getQuality() { return quality; }
    public void setQuality(String value) { quality = value; }

    private Long sampleTime;
    public Long getSampleTime() { return sampleTime; }
    public void setSampleTime(Long value) { sampleTime = value; }
}
