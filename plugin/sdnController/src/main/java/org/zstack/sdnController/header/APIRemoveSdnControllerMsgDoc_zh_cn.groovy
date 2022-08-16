package org.zstack.sdnController.header

doc {
    title "RemoveSdnController"

    category "SdnController"

    desc """删除SDN控制器"""

    rest {
        request {
			url "DELETE /v1/sdn-controllers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemoveSdnControllerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
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
            clz APIRemoveSdnControllerEvent.class
        }
    }
}