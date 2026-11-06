package org.zstack.plugin.netprobe;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * One run of a probe task; a task keeps its latest 100 results.
 */
@Entity
@Table
public class NetProbeResultVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = NetProbeTaskVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String taskUuid;

    @Column
    private Timestamp runAt;

    @Column
    private boolean success;

    @Column
    private Double latencyMs;

    @Column
    private Double lossPercent;

    @Column
    private Integer httpStatus;

    @Column
    private String error;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public Timestamp getRunAt() {
        return runAt;
    }

    public void setRunAt(Timestamp runAt) {
        this.runAt = runAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Double getLossPercent() {
        return lossPercent;
    }

    public void setLossPercent(Double lossPercent) {
        this.lossPercent = lossPercent;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
