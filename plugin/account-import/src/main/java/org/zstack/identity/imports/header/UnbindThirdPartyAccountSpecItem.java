package org.zstack.identity.imports.header;

import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;

/**
 * Created by Wenhao.Zhang on 2024/06/12
 */
public class UnbindThirdPartyAccountSpecItem {
    private String accountUuid;
    private SyncDeletedAccountStrategy strategy;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public SyncDeletedAccountStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(SyncDeletedAccountStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean needDeleteAccount() {
        return strategy == SyncDeletedAccountStrategy.DeleteAccount;
    }
}
