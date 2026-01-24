package org.zstack.header.longjob

import org.zstack.header.longjob.APISuspendLongJobEvent

doc {
    title "SuspendLongJob"

    category "longjob"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISuspendLongJobMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "suspendLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.0"
				}
			}
        }

        response {
            clz APISuspendLongJobEvent.class
        }
    }
}