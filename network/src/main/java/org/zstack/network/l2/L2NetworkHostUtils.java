package org.zstack.network.l2;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostParam;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO_;
import org.zstack.header.network.l2.L2ProviderType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class L2NetworkHostUtils {
    private static final CLogger logger = Utils.getLogger(L2NetworkHostUtils.class);

    public static void deleteL2NetworkHostRef(String l2Uuid, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return;
        }

        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuid: %s, hostUuids: %s",
                l2Uuid, hostUuids));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .delete();
    }

    public static void deleteL2NetworkHostRef(String l2Uuid, String hostUuid) {
        deleteL2NetworkHostRef(l2Uuid, Collections.singletonList(hostUuid));
    }

    public static void deleteL2NetworkHostRef(List<String> l2Uuids, String hostUuid) {
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return;
        }

        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuids: %s, hostUuid: %s",
                l2Uuids, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .delete();
    }

    public static String getBridgeNameFromL2NetworkHostRef(String l2Uuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.bridgeName)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .findValue();
    }

    public static void changeBridgeNameIfNotEqual(String l2Uuid, String hostUuid, String bridgeName) {
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .notEq(L2NetworkHostRefVO_.bridgeName, bridgeName)
                .set(L2NetworkHostRefVO_.bridgeName, bridgeName)
                .update();
    }

    public static boolean checkIfL2AttachedToHost(String l2Uuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .isExists();
    }

    public static List<String> getExcludeHostUuids(List<String> l2Uuids, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(l2Uuids) || CollectionUtils.isEmpty(hostUuids)) {
            return new ArrayList<>();
        }

        List<String> hostsWithRef = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.hostUuid)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuids)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .listValues();
        return hostUuids.stream().filter(it -> !hostsWithRef.contains(it)).collect(Collectors.toList());
    }

    public static ErrorCode validateHostParams(List<HostParam> hostParams, String clusterUuid, String hostUuid) {
        if (CollectionUtils.isEmpty(hostParams)) {
            return null;
        }

        List<String> hostUuids = new ArrayList<>();
        if (!StringUtils.isEmpty(clusterUuid)) {
            hostUuids.addAll(Q.New(HostVO.class).select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, clusterUuid)
                    .listValues());

            if (hostUuids.isEmpty()) {
                return argerr("there's no host in cluster[uuid: %s], but hostParams is set", clusterUuid);
            }
        }

        for (HostParam hostParam : hostParams) {
            if (StringUtils.isEmpty(hostParam.getHostUuid())) {
                return argerr("hostUuid can not be null in HostParam");
            }

            if (!CollectionUtils.isEmpty(hostUuids) && !hostUuids.contains(hostParam.getHostUuid())) {
                return argerr("host[uuid: %s] is not in cluster[uuid: %s]", hostParam.getHostUuid(), clusterUuid);
            }

            if (!StringUtils.isEmpty(hostUuid) && !hostUuid.equals(hostParam.getHostUuid())) {
                return argerr("hostUuid in HostParam is inconsistent with the hostUuid in APIMsg");
            }

            if (StringUtils.isEmpty(hostParam.getPhysicalInterface())) {
                return argerr("physical interface can not be null in HostParam");
            }

            if (!StringUtils.isEmpty(hostParam.getL2ProviderType())
                    && !L2ProviderType.hasType(hostParam.getL2ProviderType())) {
                return argerr("unsupported l2Network provider type[%s]", hostParam.getL2ProviderType());
            }
        }

        return null;
    }
}
