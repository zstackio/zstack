package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIDeleteL2NetworkEvent

doc {
    title "DeleteL2Network"

    category "network.l2"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/l2-networks/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDeleteL2NetworkMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
 					enclosedIn ""
 					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteL2NetworkEvent.class
        }
    }
}