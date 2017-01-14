package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetCurrentTimeReply

doc {
    title "GetCurrentTime"

    category "managementNode"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/management-nodes/actions"


            

            clz APIGetCurrentTimeMsg.class

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
            clz APIGetCurrentTimeReply.class
        }
    }
}