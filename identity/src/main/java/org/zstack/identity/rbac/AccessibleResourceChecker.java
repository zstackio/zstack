package org.zstack.identity.rbac;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 24-03-13
 *
 * replace CheckIfAccountCanAccessResource class
 */
public class AccessibleResourceChecker {
    public final String accountUuid;
    private boolean readOnly = false;
    private Class<?> resourceType;

    private AccessibleResourceChecker(String accountUuid) {
        this.accountUuid = Objects.requireNonNull(accountUuid);
    }

    public static AccessibleResourceChecker forAccount(String accountUuid) {
        return new AccessibleResourceChecker(accountUuid);
    }

    public AccessibleResourceChecker checkReadOnlyPermission() {
        this.readOnly = true;
        return this;
    }

    public AccessibleResourceChecker withResourceType(Class<?> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public boolean isAccessible(String resourceUuid) {
        return findOutAllInaccessibleResources(Collections.singletonList(resourceUuid)).isEmpty();
    }

    public List<String> findOutAllInaccessibleResources(List<String> resourceUuids) {
        if (isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }

        if (readOnly) {
            return findOutAllInaccessibleResourcesForRead(resourceUuids);
        } else {
            return findOutAllInaccessibleResourcesForWrite(resourceUuids);
        }
    }

    private List<String> findOutAllInaccessibleResourcesForRead(List<String> resourceUuids) {
        if (resourceType != null) {
            if (RBAC.isResourceGlobalReadable(resourceType)) {
                // all resources are accessible.
                return Collections.emptyList();
            } else {
                return findOutAllInaccessibleResourcesForWrite(resourceUuids);
            }
        }

        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                .in(ResourceVO_.uuid, resourceUuids)
                .listTuple();
        Set<String> types = transformToSet(tuples, tuple -> tuple.get(1, String.class));
        List<String> inaccessible = new ArrayList<>();

        for (String type : types) {
            if (RBAC.isResourceGlobalReadable(type)) {
                continue;
            }

            final List<String> filterResources = transform(
                    filter(tuples, tuple -> tuple.get(1, String.class).equals(type)),
                            tuple -> tuple.get(0, String.class));
            inaccessible.addAll(findOutAllInaccessibleResourcesForWrite(filterResources));
        }

        return inaccessible;
    }

    private List<String> findOutAllInaccessibleResourcesForWrite(List<String> resourceUuids) {
        return new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                // find out all resources the account can access
                String text = "select ref.resourceUuid from AccountResourceRefVO ref where" +
                        " ref.accountUuid = :accountUuid and type = :accessLevel" +
                        " or ref.resourceUuid in" +
                        " (select sh.resourceUuid from SharedResourceVO sh where sh.receiverAccountUuid = :accountUuid or sh.toPublic = 1)" +
                        " and ref.resourceUuid in (:uuids)";

                List<String> auuids = sql(text, String.class)
                        .param("accountUuid", accountUuid)
                        .param("accessLevel", AccessLevel.Own)
                        .param("uuids", resourceUuids)
                        .list();

                return resourceUuids.stream().filter(uuid -> !auuids.contains(uuid)).collect(Collectors.toList());
            }
        }.execute();
    }
}
