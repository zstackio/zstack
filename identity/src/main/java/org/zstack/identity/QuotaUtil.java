package org.zstack.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.QuotaVO;
import org.zstack.header.identity.QuotaVO_;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.zstack.core.Platform.err;
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
        String accountName = Q.New(AccountVO.class)
                .select(AccountVO_.name)
                .eq(AccountVO_.uuid, quotaCompareInfo.resourceTargetOwnerAccountUuid)
                .findValue();
        if (quotaCompareInfo.currentUsed + quotaCompareInfo.request > quotaCompareInfo.quotaValue) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.QUOTA_EXCEEDING,
                    "quota exceeding." +
                            "The resource owner(or target resource owner) account[uuid: %s name: %s] exceeds a quota[name: %s, value: %s], " +
                            "Current used:%s, Request:%s. Please contact the administrator.",
                    quotaCompareInfo.resourceTargetOwnerAccountUuid, StringUtils.trimToEmpty(accountName),
                    quotaCompareInfo.quotaName, quotaCompareInfo.quotaValue,
                    quotaCompareInfo.currentUsed, quotaCompareInfo.request
            ));
        }
    }

    public Map<String, Quota.QuotaPair> makeQuotaPairs(String accountUuid) {
        SimpleQuery<QuotaVO> q = dbf.createQuery(QuotaVO.class);
        q.select(QuotaVO_.name, QuotaVO_.value);
        q.add(QuotaVO_.identityType, SimpleQuery.Op.EQ, AccountVO.class.getSimpleName());
        q.add(QuotaVO_.identityUuid, SimpleQuery.Op.EQ, accountUuid);
        List<Tuple> ts = q.listTuple();

        Map<String, Quota.QuotaPair> pairs = new HashMap<>();
        for (Tuple t : ts) {
            String name = t.get(0, String.class);
            long value = t.get(1, Long.class);
            Quota.QuotaPair p = new Quota.QuotaPair();
            p.setName(name);
            p.setValue(value);
            pairs.put(name, p);
        }

        return pairs;
    }

    public AccountType getAccountType(String accountUuid) {
        SimpleQuery<AccountVO> q = dbf.createQuery(AccountVO.class);
        q.select(AccountVO_.type);
        q.add(AccountVO_.uuid, SimpleQuery.Op.EQ, accountUuid);
        return q.findValue();
    }

    public boolean isAdminAccount(String accountUuid) {
        return getAccountType(accountUuid) == AccountType.SystemAdmin;
    }

    public String getResourceType(String resourceUuid) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        AccountResourceRefVO accResRefVO = q.find();
        return accResRefVO.getResourceType();
    }

    public ErrorCode buildQuataExceedError(String currentAccountUuid, String quotaName, long quotaValue){
        return err(IdentityErrors.QUOTA_EXCEEDING,
                "quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]. Please contact the administrator.",
                currentAccountUuid, quotaName, quotaValue);
    }


    public ErrorCode buildQuataExceedError(String currentAccountUuid, String quotaName, long quotaValue, long currentUsed, long request){
        return err(IdentityErrors.QUOTA_EXCEEDING,
                "quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]," +
                        " Current used:%s, Request:%s. Please contact the administrator.",
                currentAccountUuid, quotaName, quotaValue, currentUsed, request);
    }

}
