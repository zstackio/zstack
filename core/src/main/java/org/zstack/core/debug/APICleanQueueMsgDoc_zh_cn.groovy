package org.zstack.core.debug

import org.zstack.core.debug.APICleanQueueEvent

doc {
    title "CleanQueue"

    category "debug"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/clean/queue"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICleanQueueMsg.class

            desc """"""
            
			params {

				column {
					name "signatureName"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "taskIndex"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "isCleanUp"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "0.6"
					
				}
				column {
					name "isRunningTask"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "0.6"
					
				}
				column {
					name "managementiUuid"
					enclosedIn "cleanQueue"
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
            clz APICleanQueueEvent.class
        }
    }
}