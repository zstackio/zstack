package org.zstack.identity;

import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.SessionInventory;

import java.util.Collection;
import java.util.List;

public class ResourceFilter {
    public static Collection<String> filter(Collection<String> resourceUuids, SessionInventory session) {
        if (resourceUuids.isEmpty()) {
            return resourceUuids;
        }

        return new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                List<String> ret = sql("select ref.resourceUuid from AccountResourceRefVO ref where" +
                        " (ref.accountUuid = :accountUuid and ref.resourceUuid in (:ruuids)) or ref.resourceUuid in" +
                        " (select sh.resourceUuid from SharedResourceVO sh where (sh.ownerAccountUuid = :accountUuid" +
                        " or sh.receiverAccountUuid = :accountUuid or sh.toPublic = :public) and sh.resourceUuid in (:ruuids))", String.class)
                        .param("ruuids", resourceUuids)
                        .param("accountUuid", session.getAccountUuid())
                        .param("public", true)
                        .list();
                return ret;
            }
        }.execute();
    }
}
