package org.zstack.zcenter.accounts;

public enum ZCenterAccountsErrors {
    GENERAL_ERROR(1000),

    // 2XXX: account resolution errors when exchanging session for ZCenter
    ACCOUNT_RELATED_ERROR(2000),
    ACCOUNT_NOT_FOUND(2001),
    ACCOUNT_DISABLED(2002),
    ;

    public final String code;

    ZCenterAccountsErrors(int id) {
        code = String.format("ZCENTER-ACCOUNTS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
