package org.zstack.identity;

import org.zstack.header.identity.QuotaVO;

public interface QuotaExtensionPoint {
    void beforeUpdateQuota(String quotaName, long newValue, String accountUuid);
}
