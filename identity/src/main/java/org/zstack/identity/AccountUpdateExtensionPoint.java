package org.zstack.identity;

public interface AccountUpdateExtensionPoint {
    void preUpdatePassword(String accountUuid, String currentPassword, String newPassword);

    void afterUpdatePassword(String accountUuid, String password);
}
