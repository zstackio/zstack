package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;

public class VyosVersionVersionManagerImpl implements VyosVersionManager {
    private final static CLogger logger = Utils.getLogger(VyosVersionVersionManagerImpl.class);

    @Override
    public void vyosRouterVersionCheck(String vrUuid, ReturnValueCompletion<VyosVersionCheckResult> completion) {
        VyosVersionCheckResult result = new VyosVersionCheckResult();

        String managementVersion = new VirtualRouterMetadataOperator().getManagementVersion();
        if (managementVersion == null) {
            completion.success(result);
            return;
        }

        String zvrVersion = new VirtualRouterMetadataOperator().getZvrVersion(vrUuid);
        if (zvrVersion == null) {
            logger.warn(String.format("virtual router[uuid: %s] has no zvr version tag", vrUuid));
            result.setNeedReconnect(true);
            result.setRebuildSnat(true);
            completion.success(result);
            return;
        }

        zvrVersion = zvrVersion.trim();
        if (!(VirtualRouterMetadataOperator.zvrVersionCheck(zvrVersion))) {
            logger.warn(String.format("virtual router[uuid: %s] version [%s] format error", vrUuid, zvrVersion));
            result.setNeedReconnect(true);
            result.setRebuildSnat(true);
            completion.success(result);
            return;
        }

        VersionComparator mnVersion = new VersionComparator(managementVersion);
        VersionComparator remoteVersion = new VersionComparator(zvrVersion);
        result.setVersion(zvrVersion);
        if (mnVersion.compare(remoteVersion) > 0) {
            logger.warn(String.format("virtual router[uuid: %s] version [%s] is older than management node version [%s]",vrUuid, zvrVersion, managementVersion));
            result.setNeedReconnect(true);
            if (remoteVersion.compare(VyosConstants.SNAT_REBUILD_VERSION) < 0) {
                result.setRebuildSnat(true);
            }
        } else {
            logger.debug(String.format("virtual router[uuid: %s] successfully finish the version check", vrUuid));
        }
        completion.success(result);
    }
}
