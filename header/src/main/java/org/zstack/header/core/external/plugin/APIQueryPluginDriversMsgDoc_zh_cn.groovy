package org.zstack.header.core.external.plugin

import org.zstack.header.core.external.plugin.APIQueryPluginDriversReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryPluginDrivers"

    category "external.plugin"

    desc """查询插件驱动器"""

    rest {
        request {
			url "GET /v1/external/plugins"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPluginDriversMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPluginDriversReply.class
        }
    }
}