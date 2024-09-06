package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.role.RoleAccountRefVO;
import org.zstack.header.identity.role.RoleAccountRefVO_;
import org.zstack.header.identity.role.RolePolicyChecker;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.RoleVO_;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.APIDeleteRoleMsg;
import org.zstack.header.identity.role.api.APIUpdateRoleMsg;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
<<<<<<< HEAD
=======
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.CollectionUtils;
>>>>>>> ae87856... <feature>[identity]: role policy support resource

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class RBACApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private List<RolePolicyChecker> policyCheckers = new ArrayList<>();

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteRoleMsg) {
            validate((APIDeleteRoleMsg) msg);
        } else if (msg instanceof APIUpdateRoleMsg) {
            validate((APIUpdateRoleMsg) msg);
        } else if (msg instanceof APICreateRoleMsg) {
            validate((APICreateRoleMsg) msg);
        } 

        return msg;
    }

    private void validate(APICreateRoleMsg msg) {
        msg.setFormatPolicies(transformPolicies(msg.getPolicies()));
    }

    private void validate(APIUpdateRoleMsg msg) {
        if (Q.New(RoleVO.class).in(RoleVO_.type, list(RoleType.Predefined, RoleType.System)).eq(RoleVO_.uuid, msg.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot update a system or predefined role"));
        }

        msg.setFormatPoliciesToCreate(transformPolicies(msg.getCreatePolicies()));
        msg.setFormatPoliciesToDelete(transformPolicies(msg.getDeletePolicies()));
    }


    private void validate(APIDeleteRoleMsg msg) {
        if (Q.New(RoleVO.class).in(RoleVO_.type, list(RoleType.Predefined, RoleType.System)).eq(RoleVO_.uuid, msg.getUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot delete a system or predefined role"));
        }

        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Enforcing) {
            return;
        }

        final boolean anyAttached = Q.New(RoleAccountRefVO.class)
                .eq(RoleAccountRefVO_.roleUuid, msg.getRoleUuid())
                .isExists();
        if (anyAttached) {
            throw new ApiMessageInterceptionException(
                    argerr("failed to delete role[uuid=%s]: some accounts attached this role", msg.getRoleUuid()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<RolePolicyStatement> transformPolicies(List<Object> policies) {
        if (isEmpty(policies)) {
            return Collections.emptyList();
        }

        List<RolePolicyStatement> results = new ArrayList<>(policies.size());
        for (Object policy : policies) {
            RolePolicyStatement result = null;

            if (policy instanceof String) {
                result = RolePolicyStatement.valueOf((String) policy);
            } else if (policy instanceof Map) {
                result = RolePolicyStatement.valueOf((Map<String, Object>) policy);
            }

            if (result == null || result.actions.contains(null)) {
                throw new ApiMessageInterceptionException(argerr("invalid role policy: " + policy));
            }
            results.add(result);
        }

        Map<String, List<RolePolicyStatement.Resource>> resourceMap = results.stream()
                .flatMap(statement -> statement.resources.stream())
                .collect(Collectors.groupingBy(resource -> resource.uuid));
        if (!resourceMap.isEmpty()) {
            List<Tuple> tuples = Q.New(ResourceVO.class)
                    .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                    .in(ResourceVO_.uuid, resourceMap.keySet())
                    .listTuple();
            for (Tuple tuple : tuples) {
                resourceMap.get(tuple.get(0, String.class)).forEach(it -> it.resourceType = tuple.get(1, String.class));
            }
        }

        for (RolePolicyChecker checker : policyCheckers) {
            final ErrorCode errorCode = checker.checkRolePolicies(results);
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(errorCode);
            }
        }

        return results;
    }
}
