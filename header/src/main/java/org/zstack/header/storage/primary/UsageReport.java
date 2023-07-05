package org.zstack.header.storage.primary;

import org.zstack.header.rest.SDK;

import java.util.List;

@SDK
public class UsageReport {
    List<Long> usedPhysicalCapacitiesForecast;
    List<Long> UsedPhysicalCapacitiesHistory;
    List<Long> TotalPhysicalCapacitiesHistory;
    Long startTime;
    Long interval;

    public UsageReport() {
    }

    public List<Long> getUsedPhysicalCapacitiesForecast() {
        return usedPhysicalCapacitiesForecast;
    }

    public void setUsedPhysicalCapacitiesForecast(List<Long> usedPhysicalCapacitiesForecast) {
        this.usedPhysicalCapacitiesForecast = usedPhysicalCapacitiesForecast;
    }

    public List<Long> getUsedPhysicalCapacitiesHistory() {
        return UsedPhysicalCapacitiesHistory;
    }

    public void setUsedPhysicalCapacitiesHistory(List<Long> usedPhysicalCapacitiesHistory) {
        UsedPhysicalCapacitiesHistory = usedPhysicalCapacitiesHistory;
    }

    public List<Long> getTotalPhysicalCapacitiesHistory() {
        return TotalPhysicalCapacitiesHistory;
    }

    public void setTotalPhysicalCapacitiesHistory(List<Long> totalPhysicalCapacitiesHistory) {
        TotalPhysicalCapacitiesHistory = totalPhysicalCapacitiesHistory;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }
}
