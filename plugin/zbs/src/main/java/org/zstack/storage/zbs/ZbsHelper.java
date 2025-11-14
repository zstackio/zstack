package org.zstack.storage.zbs;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.data.SizeUnit;

import java.util.function.Function;

/**
 * @author Xingwei Yu
 * @date 2024/4/9 17:50
 */
public class ZbsHelper {
    // cbdPath: cbd:physical_pool/logical_pool/volume_id
    public static String convertCbdPathToZbsPath(String cbdPath) {
        return cbdPath.replaceFirst(".+?/", "zbs://");
    }

    public static String convertZbsPathToCbdPath(String zbsPath, Function<String, String> physicalPoolGetter) {
        String logicalPool = getPoolFromVolumePath(zbsPath);
        String physicalPool = physicalPoolGetter.apply(logicalPool);
        return zbsPath.replaceFirst("zbs://", String.format("cbd:%s/", physicalPool));
    }

    public static String buildHeartbeatVolumePath(String logicalPool) {
        return String.format("zbs://%s/%s", logicalPool, ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME);
    }

    public static String buildPoolPath(String logicalPool) {
        return String.format("zbs://%s", logicalPool);
    }

    public static String buildVolumePath(String logicalPool, String volId) {
        String base = volId.replace("-", "");
        return String.format("zbs://%s/%s", logicalPool, base);
    }

    public static String getPoolFromVolumePath(String path) {
        String[] parts = path.replace("zbs://", "").split("/");
        return parts[0];
    }

    public static String getVolumeFromSnapshotPath(String path) {
        return path.split("@")[0];
    }

    public static String getSizeUnit(String version) {
        return new VersionComparator(version.split("-")[0]).compare(ZbsConstants.MEGABYTE_SUPPORTED_VERSION) >= 0 ? ZbsConstants.MEGABYTE_UNIT : ZbsConstants.DEFAULT_GIGABYTE_UNIT;
    }

    public static long alignSizeTo(long size, String unit) {
        return ZbsConstants.MEGABYTE_UNIT.equals(unit) ? (long) Math.ceil(SizeUnit.BYTE.toMegaByte((double) size)) : (long) Math.ceil(SizeUnit.BYTE.toGigaByte((double) size));
    }

    public static long convertSizeToByte(long size, String unit) {
        return ZbsConstants.MEGABYTE_UNIT.equals(unit) ? SizeUnit.MEGABYTE.toByte(size) : SizeUnit.GIGABYTE.toByte(size);
    }
}
