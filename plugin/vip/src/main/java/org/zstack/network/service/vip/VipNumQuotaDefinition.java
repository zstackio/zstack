package org.zstack.network.service.vip;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;

public class VipNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VipQuotaConstant.VIP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VipQuotaGlobalConfig.VIP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(vip) from VipVO vip, AccountResourceRefVO ref where ref.resourceUuid = vip.uuid" +
                " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", VipVO.class.getSimpleName());
        Long vn = q.find();
        vn = vn == null ? 0 : vn;
        return vn;
    }
}
