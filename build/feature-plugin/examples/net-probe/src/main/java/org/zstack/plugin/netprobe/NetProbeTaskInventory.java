package org.zstack.plugin.netprobe;

import java.sql.Timestamp;

public class NetProbeTaskInventory {
    private String uuid;
    private String name;
    private String hostUuid;
    private String type;
    private String target;
    private Integer port;
    private String url;
    private int intervalSeconds;
    private int timeoutSeconds;
    private String lastStatus;
    private Double lastLatencyMs;
    private Timestamp lastRunAt;
    private String lastError;
    private Timestamp createDate;

    public static NetProbeTaskInventory valueOf(NetProbeTaskVO vo) {
        NetProbeTaskInventory inv = new NetProbeTaskInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.hostUuid = vo.getHostUuid();
        inv.type = vo.getType();
        inv.target = vo.getTarget();
        inv.port = vo.getPort();
        inv.url = vo.getUrl();
        inv.intervalSeconds = vo.getIntervalSeconds();
        inv.timeoutSeconds = vo.getTimeoutSeconds();
        inv.lastStatus = vo.getLastStatus();
        inv.lastLatencyMs = vo.getLastLatencyMs();
        inv.lastRunAt = vo.getLastRunAt();
        inv.lastError = vo.getLastError();
        inv.createDate = vo.getCreateDate();
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public Integer getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public Double getLastLatencyMs() {
        return lastLatencyMs;
    }

    public Timestamp getLastRunAt() {
        return lastRunAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }
}
