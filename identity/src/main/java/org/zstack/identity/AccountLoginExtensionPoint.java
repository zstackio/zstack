package org.zstack.identity;

public interface AccountLoginExtensionPoint {
    AccountLoginStruct getLoginEntry(String name, String password, String type);

    default AccountLoginStruct getLoginEntry(String name, String type) {
        return null;
    }
}
