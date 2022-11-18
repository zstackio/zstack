package org.zstack.identity;

public interface AccountLoginExtensionPoint {
    AccountLoginStruct getLoginEntry(String name, String password, String type);

    AccountLoginStruct getLoginEntryByName(String name, String type);
}
