package org.zstack.header.core.progress;

import java.util.Map;

/**
 * Standardized LongJob progress detail, parsed from TaskProgressVO.opaque.
 *
 * All fields are optional (nullable). Callers should null-check before use.
 * This is a pure read-only view — the database schema (TaskProgressVO) is unchanged.
 */
public class LongJobProgressDetail {
    /** Progress percentage 0-100, if known. */
    private Integer percent;

    /** Human-readable stage label, e.g. "downloading", "extracting". */
    private String stage;

    /** State identifier, e.g. "running", "paused". */
    private String state;

    /** Human-readable reason for current state. */
    private String stateReason;

    /** Amount already processed (unit described by the {@code unit} field). */
    private Long processed;

    /** Total amount to process (unit described by the {@code unit} field). */
    private Long total;

    /** Items already processed (e.g. files, chunks). */
    private Long processedItems;

    /** Total items to process. */
    private Long totalItems;

    /** Processing speed per second (unit described by the {@code unit} field). */
    private Long speed;

    /** Unit for processed/total/speed, e.g. "bytes", "items", "steps". */
    private String unit;

    /** Estimated remaining time in seconds. */
    private Long estimatedRemainingSeconds;

    /**
     * Catch-all for any opaque fields that don't map to the standard schema.
     * Preserves unknown keys so no data is silently dropped.
     */
    private Map<String, Object> extra;

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateReason() {
        return stateReason;
    }

    public void setStateReason(String stateReason) {
        this.stateReason = stateReason;
    }

    public Long getProcessed() {
        return processed;
    }

    public void setProcessed(Long processed) {
        this.processed = processed;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(Long processedItems) {
        this.processedItems = processedItems;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getEstimatedRemainingSeconds() {
        return estimatedRemainingSeconds;
    }

    public void setEstimatedRemainingSeconds(Long estimatedRemainingSeconds) {
        this.estimatedRemainingSeconds = estimatedRemainingSeconds;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
