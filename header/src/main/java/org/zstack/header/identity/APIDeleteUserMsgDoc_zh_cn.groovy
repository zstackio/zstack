package org.zstack.header.identity

doc {
    title "DeleteUser"

    category "identity"

    desc "删除用户"

    rest {
        request {
            url "DELETE /v1/accounts/users/{uuid}"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteUserMsg.class

            desc "删除用户"

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
            clz APIDeleteUserEvent.class
        }
    }
}