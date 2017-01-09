package org.zstack.header.managementnode

org.zstack.header.managementnode.APIGetVersionReply

doc {
    title "GetVersion"

    category "managementNode"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/management-nodes/actions"


            

            clz APIGetVersionMsg.class

            desc ""
            
			params {

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
            clz APIGetVersionReply.class
        }
    }
}