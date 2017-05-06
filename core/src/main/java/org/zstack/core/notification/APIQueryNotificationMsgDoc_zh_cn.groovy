package org.zstack.core.notification

import org.zstack.core.notification.APIQueryNotificationReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryNotification"

    category "notification"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/notifications"
			url "GET /v1/notifications/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryNotificationMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryNotificationReply.class
        }
    }
}