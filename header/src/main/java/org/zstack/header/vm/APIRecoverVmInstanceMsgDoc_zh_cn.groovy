package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIRecoverVmInstanceMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "recoverVmInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIRecoverVmInstanceEvent.class
        }
    }
}