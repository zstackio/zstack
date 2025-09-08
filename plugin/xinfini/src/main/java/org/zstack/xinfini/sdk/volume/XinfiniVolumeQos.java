package org.zstack.xinfini.sdk.volume;

import org.zstack.externalStorage.sdk.ExternalStorageParam;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.header.volume.VolumeQos;
import org.zstack.xinfini.sdk.XInfiniParam;

import java.util.HashMap;
import java.util.Map;

public class XinfiniVolumeQos implements XInfiniParam {
    private static final HashMap<String, ExternalStorageParam.Parameter> parameterMap = new HashMap<>();

    @Param(required = false, numberRange = {104857600, 107374182400L})
    private long maxTotalBwBps;
    @Param(required = false, numberRange = {1000, 10000000})
    private long maxTotalIops;

    public static XinfiniVolumeQos valueOf(VolumeQos qos) {
        if (qos == null) {
            return null;
        }

        XinfiniVolumeQos ret = new XinfiniVolumeQos();
        ret.maxTotalBwBps = qos.getTotalBandwidth() == null || qos.getTotalBandwidth() < 0 ? 0 : qos.getTotalBandwidth();
        ret.maxTotalIops = qos.getTotalIOPS() == null || qos.getTotalIOPS() < 0 ? 0 : qos.getTotalIOPS();
        return ret;
    }

    public long getMaxTotalBwBps() {
        return maxTotalBwBps;
    }

    public void setMaxTotalBwBps(long maxTotalBwBps) {
        this.maxTotalBwBps = maxTotalBwBps;
    }

    public long getMaxTotalIops() {
        return maxTotalIops;
    }

    public void setMaxTotalIops(long maxTotalIops) {
        this.maxTotalIops = maxTotalIops;
    }

    @Override
    public Map<String, ExternalStorageParam.Parameter> getParameterMap() {
        return parameterMap;
    }
}
