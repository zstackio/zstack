package org.zstack.core.notification

import org.zstack.core.notification.APIQueryNotificationSubscriptionReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryNotificationSubscription"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/notifications/subscriptions"
			url "GET /v1/notifications/subscriptions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryNotificationSubscriptionMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryNotificationSubscriptionReply.class
        }
    }
}