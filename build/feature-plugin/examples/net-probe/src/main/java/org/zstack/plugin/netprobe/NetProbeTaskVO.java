package org.zstack.plugin.netprobe;

import org.zstack.header.host.HostEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * A probe a host runs every intervalSeconds; the last* fields keep the latest result for the task list.
 */
@Entity
@Table
public class NetProbeTaskVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String type;

    @Column
    private String target;

    @Column
    private Integer port;

    @Column
    private String url;

    @Column
    private int intervalSeconds;

    @Column
    private int timeoutSeconds;

    @Column
    private Timestamp nextRunAt;

    @Column
    private String lastStatus;

    @Column
    private Double lastLatencyMs;

    @Column
    private Timestamp lastRunAt;

    @Column
    private String lastError;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Timestamp getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Timestamp nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Double getLastLatencyMs() {
        return lastLatencyMs;
    }

    public void setLastLatencyMs(Double lastLatencyMs) {
        this.lastLatencyMs = lastLatencyMs;
    }

    public Timestamp getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Timestamp lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
