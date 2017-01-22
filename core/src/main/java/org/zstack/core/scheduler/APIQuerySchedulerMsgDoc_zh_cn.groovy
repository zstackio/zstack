package org.zstack.core.scheduler

import org.zstack.core.scheduler.APIQuerySchedulerReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryScheduler"

    category "core.scheduler"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/schedulers"

			url "GET /v1/schedulers/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQuerySchedulerMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySchedulerReply.class
        }
    }
}