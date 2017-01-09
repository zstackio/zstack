package org.zstack.header.console

import org.zstack.header.console.APIRequestConsoleAccessEvent

doc {
    title "RequestConsoleAccess"

    category "console"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/consoles"


            header (OAuth: 'the-session-uuid')

            clz APIRequestConsoleAccessMsg.class

            desc ""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
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
            clz APIRequestConsoleAccessEvent.class
        }
    }
}