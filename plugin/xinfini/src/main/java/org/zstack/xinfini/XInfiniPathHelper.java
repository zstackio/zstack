package org.zstack.xinfini;

/**
 * xinfini volume path like :
 * xinfini://1/2,
 * 1: pool id, 2: volume id
 **/
public class XInfiniPathHelper {

    public static String buildBdevName(String zsVolumeUuid) {
        return "volume-" + zsVolumeUuid;
    }

    public static String buildVhostControllerPath(String zsVolumeUuid) {
        return "/var/run/wds/volume-" + zsVolumeUuid;
    }

    public static String getVolumeUuidFromVhostControllerPath(String path) {
        return path.replace("/var/run/wds/volume-", "");
    }

    public static String buildUssGwName(String protocol, String managerIp) {
        return protocol + "_" + managerIp.replace(".", "_");
    }

    public static String getUssManagerIp(String name) {
        return name.split("_", 2)[1].replace("_", ".");
    }

    public static String buildXInfiniPath(Integer poolId, Integer volId) {
        return String.format("xinfini://%s/%s", poolId, volId);
    }

    public static String buildXInfiniSnapshotPath(String volPath, String snapId) {
        return volPath + "@" + snapId;
    }

    public static int getPoolIdFromPath(String url) {
        return Integer.parseInt(url.replace("xinfini://", "").split("/")[0]);
    }

    public static int getVolIdFromPath(String url) {
        return Integer.parseInt(url.replace("xinfini://", "").split("/")[1].split("@")[0]);
    }

    public static int getSnapIdFromPath(String url) {
        return Integer.parseInt(url.replace("xinfini://", "").split("/")[1].split("@")[1]);
    }
}
