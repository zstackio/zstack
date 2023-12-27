package org.zstack.crypto.securitymachine.api

import org.zstack.crypto.securitymachine.api.APIWestoneTestEvent

doc {
    title "WestoneTest"

    category "secretResourcePool"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/secret-resource-pool/westonetest"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIWestoneTestMsg.class

            desc """"""
            
			params {

				column {
					name "keyId"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "msgType"
					enclosedIn "params"
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
            clz APIWestoneTestEvent.class
        }
    }
}