package org.zstack.storage.zbs;

/**
 * @author Xingwei Yu
 * @date 2024/10/28 14:17
 */
public class ZbsNameHelper {
    private static final String ZBS_CBD_LUN_PATH_FORMAT = "cbd:%s/%s/%s";

    public static String buildVolumePath(String physicalPoolName, String logicalPoolName, String volId) {
        String base = volId.replace("-", "");
        return String.format(ZBS_CBD_LUN_PATH_FORMAT, physicalPoolName, logicalPoolName, base);
    }

    public static String getLogicalPoolNameFromPath(String url) {
        return url.split("/")[1];
    }

    public static String getPhysicalPoolNameFromPath(String url) {
        return url.split("/")[0].split(":")[1];
    }

    public static String getLunNameFromPath(String url) {
        return url.split("/")[2].split("@")[0];
    }

    public static String getSnapshotNameFromPath(String url) {
        return url.split("/")[2].split("@")[1];
    }

    public static String getVolumeInstallPathFromSnapshot(String snapshotInstallPath) {
        return snapshotInstallPath.split("@")[0];
    }
}
