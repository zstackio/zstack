package org.zstack.xinfini;

import org.zstack.header.errorcode.OperationFailureException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.operr;

/**
 * xinfini volume path like :
 * xinfini://1/2,
 * 1: pool id, 2: volume id
 **/
public class XInfiniPathHelper {

    public static String buildBdevName(String zsVolumeUuid) {
        return "volume-" + zsVolumeUuid;
    }

    public static String buildXInfiniPath(Integer poolId, Integer volId) {
        return String.format("xinfini://%s/%s", poolId, volId);
    }

    public static String buildXInfiniSnapshotPath(String volPath, Integer snapId) {
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
