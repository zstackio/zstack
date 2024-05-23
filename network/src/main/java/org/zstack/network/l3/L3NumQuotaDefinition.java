package org.zstack.network.l3;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.network.l3.L3NetworkQuotaConstant;
import org.zstack.header.network.l3.L3NetworkVO;

public class L3NumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return L3NetworkQuotaConstant.L3_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return L3NetworkQuotaGlobalConfig.L3_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(l3) from L3NetworkVO l3, AccountResourceRefVO ref where l3.uuid = ref.resourceUuid and " +
                "ref.accountUuid = :auuid and ref.resourceType = :rtype";
        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", L3NetworkVO.class.getSimpleName());
        Long l3n = q.find();
        l3n = l3n == null ? 0 : l3n;
        return l3n;
    }
}
