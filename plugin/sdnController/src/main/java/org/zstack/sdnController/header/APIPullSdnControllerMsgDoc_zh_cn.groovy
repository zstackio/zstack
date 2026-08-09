package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIPullSdnControllerEvent

doc {
    title "PullSdnController"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
            url "PUT /v1/sdn-controllers/{uuid}/resources/actions"

            header (Authorization: 'OAuth the-session-uuid')

            clz APIPullSdnControllerMsg.class

            desc """"""

            params {

                column {
                    name "uuid"
                    enclosedIn "pullSdnController"
                    desc "资源的UUID，唯一标示该资源"
                    location "url"
                    type "String"
                    optional false
                    since "5.5.38"
                }
                column {
                    name "resourceType"
                    enclosedIn "pullSdnController"
                    desc ""
                    location "body"
                    type "String"
                    optional false
                    since "5.5.38"
                    values ("Segment","TenantRouter")
                }
                column {
                    name "resourceUuids"
                    enclosedIn "pullSdnController"
                    desc ""
                    location "body"
                    type "List"
                    optional true
                    since "5.5.38"
                }
                column {
                    name "systemTags"
                    enclosedIn ""
                    desc "系统标签"
                    location "body"
                    type "List"
                    optional true
                    since "5.5.38"
                }
                column {
                    name "userTags"
                    enclosedIn ""
                    desc "用户标签"
                    location "body"
                    type "List"
                    optional true
                    since "5.5.38"
                }
            }
        }

        response {
            clz APIPullSdnControllerEvent.class
        }
    }
}