package org.zstack.identity.rbac;

import org.zstack.core.db.SQLBatchWithReturn;

import java.util.List;
import java.util.stream.Collectors;

public class CheckIfAccountCanAccessResource {
    /**
     *
     * @param resourceUuids
     * @param accountUuid
     * @return the List of resourceUuids that the account have no access
     */
    public List<String> check(List<String> resourceUuids, String accountUuid) {
        return new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                String text = "select ref.resourceUuid from AccountResourceRefVO ref where" +
                        " ref.ownerAccountUuid = :accountUuid" +
                        " or ref.resourceUuid in" +
                        " (select sh.resourceUuid from SharedResourceVO sh where sh.receiverAccountUuid = :accountUuid or sh.toPublic = 1)" +
                        " and ref.resourceUuid in (:uuids)";

                List<String> auuids = sql(text, String.class)
                        .param("accountUuid", accountUuid)
                        .param("uuids", resourceUuids)
                        .list();

                return resourceUuids.stream().filter(uuid -> !auuids.contains(uuid)).collect(Collectors.toList());
            }
        }.execute();
    }
}
