package org.zstack.core.scheduler

import org.zstack.core.scheduler.APIChangeSchedulerStateEvent

doc {
    title "ChangeSchedulerState"

    category "core.scheduler"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/schedulers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIChangeSchedulerStateMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeSchedulerState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					enclosedIn "changeSchedulerState"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
            clz APIChangeSchedulerStateEvent.class
        }
    }
}