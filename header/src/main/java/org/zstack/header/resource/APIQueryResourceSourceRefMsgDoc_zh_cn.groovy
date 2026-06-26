package org.zstack.header.resource

import org.zstack.header.resource.APIQueryResourceSourceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryResourceSourceRef"

    category "rbac"

    desc """查询资源来源引用，用于识别资源是否由 ZIAM SCIM 等外部身份源同步管理。"""

    rest {
        request {
			url "GET /v1/resources/source-refs"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryResourceSourceRefMsg.class

            desc """按资源UUID、资源类型、来源类型或同步类型查询资源来源引用。"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryResourceSourceRefReply.class
        }
    }
}