package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIUpdateSdnControllerEvent

doc {
    title "UpdateSdnController"

    category "SdnController"

    desc """更新SDN控制器"""

    rest {
        request {
			url "PUT /v1/sdn-controllers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateSdnControllerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateSdnController"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7"
					
				}
				column {
					name "name"
					enclosedIn "updateSdnController"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.7"
					
				}
				column {
					name "description"
					enclosedIn "updateSdnController"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
					
				}
			}
        }

        response {
            clz APIUpdateSdnControllerEvent.class
        }
    }
}