package org.zstack.storage.zbs;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

/**
 * @author Xingwei Yu
 * @date 2024/4/9 17:50
 */
public class ZbsHelper {
    public static void configUrl(String psUuid) {
        String psUrl = ZbsConstants.ZBS_CBD_PREFIX_SCHEME + psUuid;
        if (!psUrl.equals(Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, psUuid).findValue())) {
            SQL.New(PrimaryStorageVO.class).set(PrimaryStorageVO_.url, psUrl).eq(PrimaryStorageVO_.uuid, psUuid).update();
        }
    }

    public static String buildVolumePath(String physicalPoolName, String logicalPoolName, String volId) {
        String base = volId.replace("-", "");
        return String.format(ZbsConstants.ZBS_CBD_LUN_PATH_FORMAT, physicalPoolName, logicalPoolName, base);
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
