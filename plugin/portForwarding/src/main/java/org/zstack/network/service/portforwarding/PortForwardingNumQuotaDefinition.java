package org.zstack.network.service.portforwarding;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;

public class PortForwardingNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return PortForwardingQuotaConstant.PF_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return PortFowardingQuotaGlobalConfig.PF_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(pf) from PortForwardingRuleVO pf, AccountResourceRefVO ref where pf.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", PortForwardingRuleVO.class.getSimpleName());
        Long pfn = q.find();
        pfn = pfn == null ? 0 : pfn;
        return pfn;
    }
}
