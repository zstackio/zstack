package org.zstack.header.identity

doc {
    title "DeleteUserGroup"

    category "identity"

    desc "删除用户组"

    rest {
        request {
            url "DELETE /v1/accounts/groups/{uuid}"


            header(OAuth: 'the-session-uuid')

            clz APIDeleteUserGroupMsg.class

            desc "删除用户组"

            params {

                column {
                    name "uuid"
                    enclosedIn "params"
                    desc "资源的UUID，唯一标示该资源"
                    location "url"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "deleteMode"
                    enclosedIn "params"
                    desc "删除模式"
                    location "body"
                    type "String"
                    optional true
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
            clz APIDeleteUserGroupEvent.class
        }
    }
}