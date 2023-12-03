package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.core.db.SQL;
import org.zstack.header.identity.quota.QuotaDefinition;

public class VxlanNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VxlanNetworkQuotaConstant.VXLAN_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VxlanNetworkQuotaGlobalConfig.VXLAN_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        long cnt = SQL.New("select count(vxlan) from VxlanNetworkVO vxlan, AccountResourceRefVO ref " +
                        "where vxlan.uuid = ref.resourceUuid and ref.accountUuid = :auuid", Long.class)
                .param("auuid", accountUuid).find();
        return cnt;
    }
}
