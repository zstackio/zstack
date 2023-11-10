package org.zstack.core.upgrade

import org.zstack.core.upgrade.APIQueryAgentVersionReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryAgentVersion"

    category "core"

    desc """查询 Agent 版本信息"""

    rest {
        request {
			url "GET /v1/agent-version"
			url "GET /v1/agent-version/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryAgentVersionMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryAgentVersionReply.class
        }
    }
}