package org.zstack.header.managementnode

doc {
    title "GetVersion"

    category "managementNode"

    desc "获取当前版本"

    rest {
        request {
            url "PUT /v1/management-nodes/actions"




            clz APIGetVersionMsg.class

            desc "获取当前版本"

            params {

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
            clz APIGetVersionReply.class
        }
    }
}