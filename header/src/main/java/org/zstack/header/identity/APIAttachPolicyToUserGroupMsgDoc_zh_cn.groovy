package org.zstack.header.identity

doc {
    title "AttachPolicyToUserGroup"

    category "identity"

    desc "绑定策略到用户组"

    rest {
        request {
            url "POST /v1/accounts/groups/{groupUuid}/policies"


            header(OAuth: 'the-session-uuid')

            clz APIAttachPolicyToUserGroupMsg.class

            desc "绑定策略到用户组"

            params {

                column {
                    name "policyUuid"
                    enclosedIn ""
                    desc "权限策略UUID"
                    location "body"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "groupUuid"
                    enclosedIn ""
                    desc "用户组UUID"
                    location "url"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "systemTags"
                    enclosedIn ""
                    desc "系统标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
                column {
                    name "userTags"
                    enclosedIn ""
                    desc "用户标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
            }
        }

        response {
            clz APIAttachPolicyToUserGroupEvent.class
        }
    }
}