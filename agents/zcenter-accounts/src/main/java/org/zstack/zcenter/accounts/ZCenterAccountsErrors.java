package org.zstack.zcenter.accounts;

public enum ZCenterAccountsErrors {
    GENERAL_ERROR(1000),
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
