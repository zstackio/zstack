package org.zstack.core.config

import org.zstack.core.config.APIGetGuestOsMetadataReply

doc {
    title "GetGuestOsMetadata"

    category "globalConfig"

    desc """获取虚拟机操作系统元数据"""

    rest {
        request {
			url "GET /v1/guest-os/metadata"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetGuestOsMetadataMsg.class

            desc """"""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.1.0"
				}
			}
        }

        response {
            clz APIGetGuestOsMetadataReply.class
        }
    }
}