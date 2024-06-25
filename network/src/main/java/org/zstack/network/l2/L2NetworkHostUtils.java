package org.zstack.network.l2;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostParam;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkAttachStatus;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;

public class L2NetworkHostUtils {
    private static final CLogger logger = Utils.getLogger(L2NetworkHostUtils.class);

    public static void deleteL2NetworkHostRef(String l2NetworkUuid, List<String> hostUuids) {
        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuid:%s, hostUuids:%s",
                l2NetworkUuid, hostUuids));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .delete();
    }

    public static void changeL2NetworkToHostRefDetached(String l2NetworkUuid, String hostUuid) {
        if (checkIfL2NetworkHostRefNotExist(l2NetworkUuid, hostUuid)) {
            logger.warn(String.format("can not find host l2 network ref[l2NetworkUuid: %s, hostUuid: %s]",
                    l2NetworkUuid, hostUuid));
            return;
        }

        logger.debug(String.format("change L2NetworkHostRefVO to Detached, l2NetworkUuid:%s, hostUuid:%s",
                l2NetworkUuid, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .set(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Detached).update();
    }

    public static void changeL2NetworkToHostRefAttached(String l2NetworkUuid, String hostUuid) {
        if (checkIfL2NetworkHostRefNotExist(l2NetworkUuid, hostUuid)) {
            logger.warn(String.format("can not find host l2 network ref[l2NetworkUuid: %s, hostUuid: %s]",
                    l2NetworkUuid, hostUuid));
            return;
        }

        logger.debug(String.format("change L2NetworkHostRefVO to Attached, l2NetworkUuid:%s, hostUuid:%s",
                l2NetworkUuid, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .set(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Attached).update();
    }

    public static boolean checkIfL2NetworkHostRefNotExist(String l2NetworkUuid, String hostUuid) {
        return !Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .isExists();
    }

    public static boolean checkIfL2AttachedToHost(String l2NetworkUuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .eq(L2NetworkHostRefVO_.attachStatus, L2NetworkAttachStatus.Attached)
                .isExists();
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
        }

        return null;
    }
}
