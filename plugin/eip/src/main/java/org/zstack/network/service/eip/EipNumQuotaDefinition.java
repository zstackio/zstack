package org.zstack.network.service.eip;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;

public class EipNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return EipQuotaConstant.EIP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return EipQuotaGlobalConfig.EIP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(eip)" +
                " from EipVO eip, AccountResourceRefVO ref" +
                " where ref.resourceUuid = eip.uuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype";

        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", EipVO.class.getSimpleName());
        Long usedEipNum = q.find();
        usedEipNum = usedEipNum == null ? 0 : usedEipNum;
        return usedEipNum;
    }
}
