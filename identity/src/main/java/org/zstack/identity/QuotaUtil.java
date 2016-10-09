package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.IdentityErrors;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaUtil {
    @Autowired
    private ErrorFacade errf;

    public static class QuotaCompareInfo {
        public String currentAccountUuid;
        public String resourceTargetOwnerAccountUuid;
        public String quotaName;
        public long quotaValue;
        public long currentUsed;
        public long request;
    }

    public void CheckQuota(QuotaCompareInfo quotaCompareInfo) {
        if (quotaCompareInfo.currentUsed + quotaCompareInfo.request > quotaCompareInfo.quotaValue) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                    String.format("quota exceeding. Current account is [uuid: %s]. " +
                                    "The resource target owner account[uuid: %s] exceeds a quota[name: %s, value: %s], " +
                                    "Current used:%s, Request:%s. ",
                            quotaCompareInfo.currentAccountUuid, quotaCompareInfo.resourceTargetOwnerAccountUuid,
                            quotaCompareInfo.quotaName, quotaCompareInfo.quotaValue,
                            quotaCompareInfo.currentUsed, quotaCompareInfo.request)
            ));
        }
    }
}
