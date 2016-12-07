package org.zstack.header.apimediator



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/management-nodes/ready"


            header (OAuth: 'the-session-uuid')

            clz APIIsReadyToGoMsg.class

            desc ""
            
			params {

				column {
					name "managementNodeId"
					enclosedIn "params"
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIIsReadyToGoReply.class
        }
    }
}