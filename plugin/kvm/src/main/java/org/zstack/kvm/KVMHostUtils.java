package org.zstack.kvm;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 4/16/20.
 */
public class KVMHostUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(KVMHostUtils.class);

    /**
     * Get normalized bridge name for l2 network, which at most has 15 chars.
     * - if l2 network has L2_BRIDGE_NAME tag, then return it's value directly;
     * - if l2Uuid does not have an L2_BRIDGE_NAME tag, use the new naming convention:
     *   prefix 'l2_' plus the last 12 characters of l2Uuid;
     * - if l2 physical interface name is short, then no need for truncation;
     * - otherwise, get md5sum of interface name and use the top chars.
     * @param l2Uuid l2 network uuid
     * @param format bridge name format string, like "br_%s", "br_%s_100" (only one '%s' is allowed)
     * @return normalized bridge name, or null if anything wrong
     */
    public static String getNormalizedBridgeName(String l2Uuid, String format) {
        if (KVMSystemTags.L2_BRIDGE_NAME.hasTag(l2Uuid, L2NetworkVO.class)) {
            return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }
        String newBridgeName = "l2_" + l2Uuid.substring(Math.max(0, l2Uuid.length() - 12));

        if (!format.contains("%s") || format.indexOf("%s") != format.lastIndexOf("%s")) {
            throw new OperationFailureException(operr("invalid format string %s", format));
        }

        int allowedLen = L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE - format.length() + "%s".length();

        String physicalInterface = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .select(L2NetworkVO_.physicalInterface)
                .findValue();

        if (physicalInterface != null && physicalInterface.length() > allowedLen) {
            physicalInterface = DigestUtils.md5Hex(physicalInterface).substring(0, allowedLen);
        }

        return newBridgeName;
    }
}
