package org.zstack.header.image

doc {
    title "修改镜像状态(ChangeImageState)"

    category "image"

    desc "修改镜像状态"

    rest {
        request {
            url "PUT /v1/images/{uuid}/actions"


            header(OAuth: 'the-session-uuid')

            clz APIChangeImageStateMsg.class

            desc ""

            params {

                column {
                    name "uuid"
                    enclosedIn "params"
                    desc "镜像的UUID，唯一标示该镜像"
                    location "url"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "stateEvent"
                    enclosedIn "params"
                    desc "镜像的状态"
                    location "body"
                    type "String"
                    optional false
                    since "0.6"
                    values("enable", "disable")
                }
                column {
                    name "systemTags"
                    enclosedIn "params"
                    desc "系统标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
                column {
                    name "userTags"
                    enclosedIn "params"
                    desc "用户标签"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

                }
            }
        }

        response {
            clz APIChangeImageStateEvent.class
        }
    }
}