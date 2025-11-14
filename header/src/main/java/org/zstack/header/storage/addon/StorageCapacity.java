package org.zstack.header.storage.addon;

import java.util.HashMap;
import java.util.Map;

public class StorageCapacity {
    public static class Capacity {
        public long total;
        public long available;
    }

    private long totalCapacity;
    private long availableCapacity;
    /***
     * key: location url, it must be unique and consistent anywhere and anytime.
     * ZStack use location url to identify different storage locations and generate storage space resource uuid based on it.
     * storage space resource uuid = UUID.nameUUIDFromBytes((psUuid + locationUrl).getBytes()).toString()
     * <p>
     * if storage do not support multiple locations, return null or empty map.
     * if storage support multiple locations, the location url must be the prefix of volume install url.
     * ZStack will retrieve the location url from volume install url to find the corresponding capacity info.
     */
    private Map<String, Capacity> capacitiesByLocationUrl;

    // remove in future versions
    @Deprecated
    private StorageHealthy healthy;

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Map<String, Capacity> getCapacitiesByLocationUrl() {
        return capacitiesByLocationUrl;
    }

    public void setCapacitiesByLocationUrl(Map<String, Capacity> capacitiesByLocationUrl) {
        this.capacitiesByLocationUrl = capacitiesByLocationUrl;
    }

    public void putCapacity(String locationUrl, long availableCapacity, long totalCapacity) {
        if (this.capacitiesByLocationUrl == null) {
            this.capacitiesByLocationUrl = new HashMap<>();
        }

        Capacity capacity = new Capacity();
        capacity.available = availableCapacity;
        capacity.total = totalCapacity;
        this.capacitiesByLocationUrl.put(locationUrl, capacity);
    }

    public StorageHealthy getHealthy() {
        return healthy;
    }

    public void setHealthy(StorageHealthy healthy) {
        this.healthy = healthy;
    }
}
