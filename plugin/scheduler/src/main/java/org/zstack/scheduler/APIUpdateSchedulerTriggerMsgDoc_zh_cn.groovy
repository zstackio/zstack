package org.zstack.scheduler

import org.zstack.scheduler.APIUpdateSchedulerTriggerEvent

doc {
    title "UpdateSchedulerTrigger"

    category "scheduler"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/scheduler/triggers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateSchedulerTriggerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateSchedulerTrigger"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateSchedulerTrigger"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateSchedulerTrigger"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
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
            clz APIUpdateSchedulerTriggerEvent.class
        }
    }
}