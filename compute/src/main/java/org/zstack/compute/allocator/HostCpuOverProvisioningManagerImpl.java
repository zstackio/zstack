package org.zstack.compute.allocator;

import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xing5 on 2016/5/12.
 */
public class HostCpuOverProvisioningManagerImpl implements HostCpuOverProvisioningManager {
    private Integer globalRatio;
    private ConcurrentHashMap<String, Integer> ratios = new ConcurrentHashMap<String, Integer>();

    @Override
    public void setGlobalRatio(int ratio) {
        globalRatio = ratio;
    }

    @Override
    public int getGlobalRatio() {
        return globalRatio == null ? HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.value(Integer.class) : globalRatio;
    }

    @Override
    public void setRatio(String hostUuid, int ratio) {
        ratios.put(hostUuid, ratio);
    }

    @Override
    public void deleteRatio(String hostUuid) {
        ratios.remove(hostUuid);
    }

    @Override
    public int getRatio(String hostUuid) {
        Integer r = ratios.get(hostUuid);
        return r == null ? getGlobalRatio() : r;
    }

    @Override
    public Map<String, Integer> getAllRatio() {
        return ratios;
    }

    @Override
    public int calculateByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        int ret = Math.round(cpuNum / r);
        return ret == 0 ? 1 : ret;
    }

    @Override
    public int calculateHostCpuByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        return cpuNum * r;
    }
}
