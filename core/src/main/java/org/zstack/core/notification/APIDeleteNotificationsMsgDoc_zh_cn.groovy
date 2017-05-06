package org.zstack.core.notification

import org.zstack.core.notification.APIDeleteNotificationsEvent

doc {
    title "DeleteNotifications"

    category "notification"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/notifications"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIDeleteNotificationsMsg.class

            desc """"""
            
			params {

				column {
					name "uuids"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
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
            clz APIDeleteNotificationsEvent.class
        }
    }
}