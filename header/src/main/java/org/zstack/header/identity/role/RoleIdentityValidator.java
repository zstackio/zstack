package org.zstack.header.identity.role;

import org.zstack.header.identity.PolicyStatement;

import java.util.List;

public interface RoleIdentityValidator {
    void validateRolePolicy(RoleIdentity roleIdentity, List<PolicyStatement> policyStatements);
}
