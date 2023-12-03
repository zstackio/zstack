package org.zstack.network.securitygroup;

import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.quota.QuotaDefinition;

public class SecurityGroupNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return SecurityGroupQuotaConstant.SG_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SecurityGroupQuotaGlobalConfig.SG_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                .eq(AccountResourceRefVO_.resourceType, SecurityGroupVO.class.getSimpleName())
                .count();
    }
}
