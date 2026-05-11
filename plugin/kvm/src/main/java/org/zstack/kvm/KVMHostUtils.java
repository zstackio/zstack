package org.zstack.kvm;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.Locale;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 4/16/20.
 */
public class KVMHostUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(KVMHostUtils.class);

    public static boolean shouldContinueReconnectOnAnsibleFailure(boolean isNewAdded, ErrorCode errorCode) {
        return !isNewAdded && isLibvirtSocketMaskSystemdTimeout(errorCode);
    }

    public static boolean isLibvirtSocketMaskSystemdTimeout(ErrorCode errorCode) {
        String errorText = collectErrorText(errorCode).toLowerCase(Locale.ROOT);
        return errorText.contains("systemctl mask")
                && errorText.contains("libvirtd.socket")
                && errorText.contains("org.freedesktop.systemd1")
                && errorText.contains("timed out")
                && (errorText.contains("failed to get properties")
                    || errorText.contains("failed to activate service"));
    }

    private static String collectErrorText(ErrorCode errorCode) {
        StringBuilder sb = new StringBuilder();
        ErrorCode cursor = errorCode;
        while (cursor != null) {
            appendIfNotNull(sb, cursor.getDetails());
            appendIfNotNull(sb, cursor.getDescription());
            cursor = cursor.getCause();
        }
        return sb.toString();
    }

    private static void appendIfNotNull(StringBuilder sb, String text) {
        if (text != null) {
            sb.append(text).append('\n');
        }
    }

    /**
     * Get normalized bridge name for l2 network, which at most has 15 chars.
     * - if l2 network has L2_BRIDGE_NAME tag, then return it's value directly;
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

        return String.format(format, physicalInterface);
    }
}
