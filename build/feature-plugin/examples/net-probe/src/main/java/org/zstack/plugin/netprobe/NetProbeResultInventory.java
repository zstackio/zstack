package org.zstack.plugin.netprobe;

import java.sql.Timestamp;

public class NetProbeResultInventory {
    private long id;
    private String taskUuid;
    private Timestamp runAt;
    private boolean success;
    private Double latencyMs;
    private Double lossPercent;
    private Integer httpStatus;
    private String error;

    public static NetProbeResultInventory valueOf(NetProbeResultVO vo) {
        NetProbeResultInventory inv = new NetProbeResultInventory();
        inv.id = vo.getId();
        inv.taskUuid = vo.getTaskUuid();
        inv.runAt = vo.getRunAt();
        inv.success = vo.isSuccess();
        inv.latencyMs = vo.getLatencyMs();
        inv.lossPercent = vo.getLossPercent();
        inv.httpStatus = vo.getHttpStatus();
        inv.error = vo.getError();
        return inv;
    }

    public long getId() {
        return id;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public Timestamp getRunAt() {
        return runAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public Double getLossPercent() {
        return lossPercent;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getError() {
        return error;
    }
}
