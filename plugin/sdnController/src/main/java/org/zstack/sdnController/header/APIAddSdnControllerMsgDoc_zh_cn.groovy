package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIAddSdnControllerEvent

doc {
    title "AddSdnController"

    category "SdnController"

    desc """添加SDN控制器"""

    rest {
        request {
			url "POST /v1/sdn-controllers"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddSdnControllerMsg.class

            desc """"""
            
			params {

				column {
					name "vendorType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "ip"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "userName"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "password"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
            clz APIAddSdnControllerEvent.class
        }
    }
}