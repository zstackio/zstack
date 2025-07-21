package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIPullSdnControllerTenantEvent

doc {
    title "PullSdnControllerTenant"

    category "SdnController"

    desc """拉取SDN控制器租户"""

    rest {
        request {
			url "PUT /v1/sdn-controllers/{uuid}/tenant/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPullSdnControllerTenantMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "pullSdnControllerTenant"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.3.28"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.3.28"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.3.28"
				}
			}
        }

        response {
            clz APIPullSdnControllerTenantEvent.class
        }
    }
}