package org.zstack.kvm;

import org.apache.commons.codec.digest.DigestUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l2.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.TagUtils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 4/16/20.
 */
public class KVMHostUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(KVMHostUtils.class);

    public static final long LIBVIRT_RESTART_ECHO_TIMEOUT_VM_THRESHOLD = 100;
    public static final long LIBVIRT_RESTART_ECHO_TIMEOUT_PER_VM_SECONDS = 1;
    public static final long LIBVIRT_RESTART_ECHO_TIMEOUT_MAX_SECONDS = 180;

    public static boolean shouldRestartLibvirtdDuringDeploy(String init, String restartLibvirtd) {
        return "true".equalsIgnoreCase(init) || "true".equalsIgnoreCase(restartLibvirtd);
    }

    public static long countVmsForLibvirtRestartEchoTimeout(String hostUuid) {
        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .notEq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .count();
    }

    public static long calculateLibvirtRestartEchoTimeoutMillis(long vmCount) {
        long defaultTimeoutSeconds = CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT;
        if (vmCount <= LIBVIRT_RESTART_ECHO_TIMEOUT_VM_THRESHOLD) {
            return TimeUnit.SECONDS.toMillis(defaultTimeoutSeconds);
        }

        long maxExtraSeconds = Math.max(0, LIBVIRT_RESTART_ECHO_TIMEOUT_MAX_SECONDS - defaultTimeoutSeconds);
        long extraVmCount = vmCount - LIBVIRT_RESTART_ECHO_TIMEOUT_VM_THRESHOLD;
        long extraSeconds = Math.min(extraVmCount * LIBVIRT_RESTART_ECHO_TIMEOUT_PER_VM_SECONDS, maxExtraSeconds);
        return TimeUnit.SECONDS.toMillis(defaultTimeoutSeconds + extraSeconds);
    }

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
     * - if l2Uuid does not have an L2_BRIDGE_NAME tag and conflict with existing bridge name,
     * use the new naming convention : prefix 'l2_' plus the last 12 characters of l2Uuid;
     * - if l2 physical interface name is short, then no need for truncation;
     * - otherwise, get md5sum of interface name and use the top chars.
     * @param l2Uuid l2 network uuid
     * @param format bridge name format string, like "br_%s", "br_%s_100" (only one '%s' is allowed)
     * @return normalized bridge name, or null if anything wrong
     */
    public static String getNormalizedBridgeName(String l2Uuid, String format) {
        String current = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (current != null) {
            return current;
        }

        validateFormatString(format);

        String physicalInterface = getPhysicalInterface(l2Uuid, format);
        String preferredBridgeName = String.format(format, physicalInterface);

        if (!checkNameConflict(l2Uuid, preferredBridgeName)) {
            return preferredBridgeName;
        }

        current = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (current != null) {
            return current;
        }

        return generateNewBridgeName(l2Uuid);
    }

    private static void validateFormatString(String format) {
        if (!format.contains("%s") || format.indexOf("%s") != format.lastIndexOf("%s")) {
            throw new IllegalArgumentException(String.format("invalid format string: %s", format));
        }
    }

    private static String generateNewBridgeName(String l2Uuid) {
        return "l2_" + l2Uuid.substring(0, Math.min(l2Uuid.length(), 12));
    }

    private static String getPhysicalInterface(String l2Uuid, String format) {
        String physicalInterface = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .select(L2NetworkVO_.physicalInterface)
                .findValue();

        int allowedLen = L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE - format.length() + 2; // "%s" length is 2

        if (physicalInterface != null && physicalInterface.length() > allowedLen) {
            physicalInterface = DigestUtils.md5Hex(physicalInterface).substring(0, allowedLen);
        }
        return physicalInterface;
    }

    public static Boolean checkNameConflict(String l2Uuid, String bridgeName) {
        String pattern = TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.instantiateTag(
                Collections.singletonMap(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, bridgeName)
        ));
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid).listValues();
        if (clusterUuids.isEmpty()) {
            return Boolean.FALSE;
        }
        Set<String> relatedL2Uuids = new HashSet<>(Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .in(L2NetworkClusterRefVO_.clusterUuid, clusterUuids).listValues());
        List<SystemTagVO> tags = Q.New(SystemTagVO.class)
                .in(SystemTagVO_.resourceUuid, relatedL2Uuids)
                .eq(SystemTagVO_.resourceType, L2NetworkVO.class.getSimpleName())
                .like(SystemTagVO_.tag, pattern)
                .eq(SystemTagVO_.type, TagType.System)
                .list();

        return !tags.isEmpty();
    }
}
