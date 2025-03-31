package org.zstack.identity;

import org.zstack.core.db.Q;
import org.zstack.header.identity.*;

public interface Account {
    static String getAccountUuidOfResource(String resUuid) {
        return Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, resUuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .findValue();
    }

    static boolean isAdminPermission(SessionInventory session) {
        return isAdminPermission(session.getAccountUuid());
    }

    static boolean isAdminPermission(String accountUuid) {
        if (isAdmin(accountUuid)) {
            return true;
        }

        return Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, accountUuid)
                .eq(AccountVO_.type, AccountType.SystemAdmin)
                .isExists();
    }

    static boolean isAdmin(SessionInventory session) {
        return AccountConstant.isAdmin(session.getAccountUuid());
    }

    static boolean isAdmin(String accountUuid) {
        return AccountConstant.isAdmin(accountUuid);
    }
}
