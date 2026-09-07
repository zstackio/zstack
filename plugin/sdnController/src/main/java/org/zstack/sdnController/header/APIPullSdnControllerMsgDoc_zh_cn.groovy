package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIPullSdnControllerEvent

doc {
    title "PullSdnController"

    category "SdnController"

    desc """从SDN控制器拉取指定类型的最新资源"""

    rest {
        request {
			url "PUT /v1/sdn-controllers/{uuid}/resources/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPullSdnControllerMsg.class

            desc """触发Cloud从指定SDN控制器读取Segment或TenantRouter；可指定资源UUID列表，未指定时拉取该类型的全部资源"""
            
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
					desc "待拉取的资源类型，支持Segment或TenantRouter"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("Segment","TenantRouter")
				}
				column {
					name "resourceUuids"
					enclosedIn "pullSdnController"
					desc "待拉取的资源UUID列表；为空时拉取所选类型的全部资源"
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