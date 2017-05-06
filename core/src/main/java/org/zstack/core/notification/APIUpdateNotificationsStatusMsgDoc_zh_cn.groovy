package org.zstack.core.notification

import org.zstack.core.notification.APIUpdateNotificationsStatusEvent

doc {
    title "UpdateNotificationsStatus"

    category "notification"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/notifications/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIUpdateNotificationsStatusMsg.class

            desc """"""
            
			params {

				column {
					name "uuids"
					enclosedIn "updateNotificationsStatus"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "status"
					enclosedIn "updateNotificationsStatus"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("Unread","Read")
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
            clz APIUpdateNotificationsStatusEvent.class
        }
    }
}