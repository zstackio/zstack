package org.zstack.header.identity;

public interface IdentityRoles {
    public static final String ALL_ACCOUNT_ROLES = "identity:.*";
    public static final String CREATE_ACCOUNT_ROLE = "identity:createAccount";
    public static final String RESET_ACCOUNT_PASSWORD_ROLE = "identity:resetAccountPassword";
    public static final String CREATE_USER_ROLE = "identity:createUser";
    public static final String RESET_USER_PASSWORD_ROLE = "identity:resetUserPassword";
    public static final String CREATE_POLICY_ROLE = "identity:createPolicy";
    public static final String ATTACH_POLICY_TO_USER_ROLE = "identity:attachPolicyToUser";
    public static final String CREATE_USER_GROUP_ROLE = "identity:createUserGroup";
    public static final String ATTACH_POLICY_TO_USER_GROUP_ROLE = "identity:attachPolicyToUserGroup";
    public static final String ATTACH_USER_TO_USER_GROUP_ROLE = "identity:attachUserToUserGroup";
    public static final String SEARCH_USER_ROLE = "identity:searchUser";
    public static final String SEARCH_POLICY_ROLE = "identity:searchPolicy";
    public static final String SEARCH_USER_GROUP_ROLE = "identity:searchUserGroup";
}
