package org.zstack.header.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Capability-model workload status (role SPI v3, 2026-04-16). Replaces v2's
 * {@code checkBeforeDetach(serverUuid, roleUuid): String} which was hard-coded for a single
 * destructive operation. Each {@code *BlockReason} field is {@code null} when the operation is
 * permitted, non-null with a human-readable reason when it is blocked. New destructive
 * operations are added by extending this class with new fields — the SPI signature does not
 * change.
 */
public class RoleWorkloadStatus {
    private int activeWorkloadCount;
    private List<WorkloadRef> activeWorkloads = new ArrayList<>();

    private String detachBlockReason;
    private String powerOffBlockReason;
    private String powerResetBlockReason;
    private String maintenanceBlockReason;
    private String migrationBlockReason;

    public int getActiveWorkloadCount() { return activeWorkloadCount; }
    public void setActiveWorkloadCount(int activeWorkloadCount) { this.activeWorkloadCount = activeWorkloadCount; }

    public List<WorkloadRef> getActiveWorkloads() { return activeWorkloads; }
    public void setActiveWorkloads(List<WorkloadRef> activeWorkloads) {
        this.activeWorkloads = activeWorkloads == null ? new ArrayList<>() : activeWorkloads;
    }

    public String getDetachBlockReason() { return detachBlockReason; }
    public void setDetachBlockReason(String detachBlockReason) { this.detachBlockReason = detachBlockReason; }

    public String getPowerOffBlockReason() { return powerOffBlockReason; }
    public void setPowerOffBlockReason(String powerOffBlockReason) { this.powerOffBlockReason = powerOffBlockReason; }

    public String getPowerResetBlockReason() { return powerResetBlockReason; }
    public void setPowerResetBlockReason(String powerResetBlockReason) { this.powerResetBlockReason = powerResetBlockReason; }

    public String getMaintenanceBlockReason() { return maintenanceBlockReason; }
    public void setMaintenanceBlockReason(String maintenanceBlockReason) { this.maintenanceBlockReason = maintenanceBlockReason; }

    public String getMigrationBlockReason() { return migrationBlockReason; }
    public void setMigrationBlockReason(String migrationBlockReason) { this.migrationBlockReason = migrationBlockReason; }
}
