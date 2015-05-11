package org.zstack.header.allocator;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.Message;

public class ReturnHostCapacityMsg extends Message {
    private long cpuCapacity;
    private long memoryCapacity;
    private HostInventory host;

    public long getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(long cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }
}
