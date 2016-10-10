package org.zstack.identity;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.IdentityErrors;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaUtil {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    public static class QuotaCompareInfo {
        public String currentAccountUuid;
        public String resourceTargetOwnerAccountUuid;
        public String quotaName;
        public long quotaValue;
        public long currentUsed;
        public long request;
    }

    @Transactional(readOnly = true)
    public String getResourceOwnerAccountUuid(String resourceUuid) {
        SimpleQuery<AccountResourceRefVO> q;
        q = dbf.createQuery(AccountResourceRefVO.class);
        q.select(AccountResourceRefVO_.ownerAccountUuid);
        q.add(AccountResourceRefVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String owner = q.findValue();
        if (owner == null || owner.equals("")) {
            throw new CloudRuntimeException(
                    String.format("cannot find owner account uuid for resource[uuid:%s]", resourceUuid));
        } else {
            return owner;
        }
    }

    public void CheckQuota(QuotaCompareInfo quotaCompareInfo) {
        Assert.assertNotNull(quotaCompareInfo);
        Assert.assertNotNull(quotaCompareInfo.currentAccountUuid);
        Assert.assertNotNull(quotaCompareInfo.resourceTargetOwnerAccountUuid);
        Assert.assertNotNull(quotaCompareInfo.quotaName);
        Assert.assertNotNull(quotaCompareInfo.quotaValue);
        Assert.assertNotNull(quotaCompareInfo.currentUsed);
        Assert.assertNotNull(quotaCompareInfo.request);


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
