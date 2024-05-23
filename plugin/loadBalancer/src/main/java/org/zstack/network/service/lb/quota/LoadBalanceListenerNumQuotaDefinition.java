package org.zstack.network.service.lb.quota;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.network.service.lb.LoadBalanceQuotaConstant;
import org.zstack.network.service.lb.LoadBalanceQuotaGlobalConfig;
import org.zstack.network.service.lb.LoadBalancerListenerVO;

public class LoadBalanceListenerNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return LoadBalanceQuotaGlobalConfig.LOAD_BALANCER_LISTENER_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(lb.uuid) from LoadBalancerListenerVO lb, AccountResourceRefVO ref where ref.resourceUuid = lb.uuid and " +
                "ref.accountUuid = :auuid and ref.resourceType = :rtype";

        SQL q = SQL.New(sql, Long.class);
        q.param("auuid", accountUuid);
        q.param("rtype", LoadBalancerListenerVO.class.getSimpleName());
        Long en = q.find();
        en = en == null ? 0 : en;
        return en;
    }
}
