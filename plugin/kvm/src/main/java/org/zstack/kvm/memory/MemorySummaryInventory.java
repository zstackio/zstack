package org.zstack.kvm.memory;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public API inventory. Nullable samples represent unknown, never a fabricated zero. */
public class MemorySummaryInventory {
    public static MemorySummaryInventory __example__() {
        MemorySummaryInventory inventory = new MemorySummaryInventory();
        inventory.setSampleTime(1791583200000L);
        inventory.setTotalSavedEstimateBytes(1610612736L);
        inventory.setKsmOrdinaryBytes(536870912L);
        inventory.setKsmZeroBytes(268435456L);
        inventory.setKsmTotalBytes(805306368L);
        inventory.setZramBytes(805306368L);
        inventory.setZramNormalBytes(671088640L);
        inventory.setZramWritebackBytes(134217728L);
        inventory.setExpectedHosts(2);
        inventory.setCoveredHosts(2);
        inventory.setQuality("COMPLETE");
        inventory.setCurrentStatus(java.util.Collections.singletonMap("APPLIED", 2));
        inventory.setCurrentStatusSampleTime(1791583200000L);
        inventory.setCurrentStatusTtlMillis(60000L);
        return inventory;
    }

    private String formulaVersion = "mechanism-estimate-v1";
    public String getFormulaVersion() { return formulaVersion; }
    public void setFormulaVersion(String value) { formulaVersion = value; }

    private Long sampleTime;
    public Long getSampleTime() { return sampleTime; }
    public void setSampleTime(Long value) { sampleTime = value; }

    private Long totalSavedEstimateBytes;
    public Long getTotalSavedEstimateBytes() { return totalSavedEstimateBytes; }
    public void setTotalSavedEstimateBytes(Long value) { totalSavedEstimateBytes = value; }

    private Long ksmOrdinaryBytes;
    public Long getKsmOrdinaryBytes() { return ksmOrdinaryBytes; }
    public void setKsmOrdinaryBytes(Long value) { ksmOrdinaryBytes = value; }

    private Long ksmZeroBytes;
    public Long getKsmZeroBytes() { return ksmZeroBytes; }
    public void setKsmZeroBytes(Long value) { ksmZeroBytes = value; }

    private Long ksmTotalBytes;
    public Long getKsmTotalBytes() { return ksmTotalBytes; }
    public void setKsmTotalBytes(Long value) { ksmTotalBytes = value; }

    private Long zramBytes;
    public Long getZramBytes() { return zramBytes; }
    public void setZramBytes(Long value) { zramBytes = value; }

    /** Resident (non-writeback) ZRAM savings in the same native sample. */
    private Long zramNormalBytes;
    public Long getZramNormalBytes() { return zramNormalBytes; }
    public void setZramNormalBytes(Long value) { zramNormalBytes = value; }

    /** Live writeback logical bytes, never cumulative write I/O. */
    private Long zramWritebackBytes;
    public Long getZramWritebackBytes() { return zramWritebackBytes; }
    public void setZramWritebackBytes(Long value) { zramWritebackBytes = value; }

    private int expectedHosts;
    public int getExpectedHosts() { return expectedHosts; }
    public void setExpectedHosts(int value) { expectedHosts = value; }

    private int coveredHosts;
    public int getCoveredHosts() { return coveredHosts; }
    public void setCoveredHosts(int value) { coveredHosts = value; }

    private String quality;
    public String getQuality() { return quality; }
    public void setQuality(String value) { quality = value; }
    private Map<String, Map<String, Object>> metricCoverage = new LinkedHashMap<>();
    public Map<String, Map<String, Object>> getMetricCoverage() { return metricCoverage; }
    public void setMetricCoverage(Map<String, Map<String, Object>> value) {
        metricCoverage = value == null ? new LinkedHashMap<>() : value;
    }

    /**
     * Counts of explicitly evidenced current host states.  A category is not
     * inferred from an expired sample; absent evidence is reported as unknown.
     */
    private Map<String, Integer> currentStatus = new LinkedHashMap<>();
    private Long currentStatusSampleTime;
    public Long getCurrentStatusSampleTime() { return currentStatusSampleTime; }
    public void setCurrentStatusSampleTime(Long value) { currentStatusSampleTime = value; }
    private Long currentStatusTtlMillis;
    public Long getCurrentStatusTtlMillis() { return currentStatusTtlMillis; }
    public void setCurrentStatusTtlMillis(Long value) { currentStatusTtlMillis = value; }
    public Map<String, Integer> getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(Map<String, Integer> value) {
        currentStatus = value == null ? new LinkedHashMap<>() : value;
    }

}
