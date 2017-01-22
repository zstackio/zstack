package org.zstack.core.scheduler

import org.zstack.core.scheduler.APIUpdateSchedulerEvent

doc {
    title "UpdateScheduler"

    category "core.scheduler"

    desc "更新定时任务"

    rest {
        request {
			url "PUT /v1/schedulers/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIUpdateSchedulerMsg.class

			desc "更新定时任务"

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
					name "schedulerName"
					enclosedIn ""
					desc "定时任务名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "schedulerDescription"
					enclosedIn ""
					desc "定时任务描述"
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
            clz APIUpdateSchedulerEvent.class
        }
    }
}