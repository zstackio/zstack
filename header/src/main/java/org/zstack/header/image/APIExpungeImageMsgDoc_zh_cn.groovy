package org.zstack.header.image

doc {
    title "彻底删除镜像(ExpungeImage)"

    category "image"

    desc "彻底删除镜像"

    rest {
        request {
            url "PUT /v1/images/{imageUuid}/actions"


            header(OAuth: 'the-session-uuid')

            clz APIExpungeImageMsg.class

            desc ""

            params {

                column {
                    name "imageUuid"
                    enclosedIn "params"
                    desc "镜像UUID"
                    location "url"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "backupStorageUuids"
                    enclosedIn "params"
                    desc "镜像服务器UUID列表"
                    location "body"
                    type "List"
                    optional true
                    since "0.6"

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
            clz APIExpungeImageEvent.class
        }
    }
}