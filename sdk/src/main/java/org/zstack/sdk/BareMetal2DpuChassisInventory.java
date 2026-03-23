package org.zstack.sdk;

import org.zstack.sdk.BareMetal2DpuChassisConfig;
import org.zstack.sdk.BareMetal2DpuHostInventory;

public class BareMetal2DpuChassisInventory extends org.zstack.sdk.BareMetal2ChassisInventory {

    public BareMetal2DpuChassisConfig config;
    public void setConfig(BareMetal2DpuChassisConfig config) {
        this.config = config;
    }
    public BareMetal2DpuChassisConfig getConfig() {
        return this.config;
    }

    public BareMetal2DpuHostInventory dpuHost;
    public void setDpuHost(BareMetal2DpuHostInventory dpuHost) {
        this.dpuHost = dpuHost;
    }
    public BareMetal2DpuHostInventory getDpuHost() {
        return this.dpuHost;
    }

}
