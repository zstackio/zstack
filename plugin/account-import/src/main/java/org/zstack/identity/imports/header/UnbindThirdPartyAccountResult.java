package org.zstack.identity.imports.header;

import org.zstack.header.errorcode.ErrorCode;

public class UnbindThirdPartyAccountResult {
    private String accountUuid;
    private ErrorCode errorForDeleteAccount;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public ErrorCode getErrorForDeleteAccount() {
        return errorForDeleteAccount;
    }

    public void setErrorForDeleteAccount(ErrorCode errorForDeleteAccount) {
        this.errorForDeleteAccount = errorForDeleteAccount;
    }
}
