package org.zstack.core.config

import org.zstack.core.config.APIQueryGlobalConfigReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryGlobalConfig"

    category "globalConfig"

    desc "查询全局配置"

    rest {
        request {
			url "GET /v1/global-configurations"


            header (OAuth: 'the-session-uuid')

            clz APIQueryGlobalConfigMsg.class

            desc "查询全局配置"
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryGlobalConfigReply.class
        }
    }
}