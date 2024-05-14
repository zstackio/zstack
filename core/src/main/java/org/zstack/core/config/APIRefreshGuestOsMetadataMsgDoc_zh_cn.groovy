package org.zstack.core.config

import org.zstack.core.config.APIRefreshGuestOsMetadataEvent

doc {
    title "RefreshGuestOsMetadata"

    category "globalConfig"

    desc """刷新虚拟机操作系统元数据"""

    rest {
        request {
			url "PUT /v1/guest-os/metadata/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRefreshGuestOsMetadataMsg.class

            desc """"""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
			}
        }

        response {
            clz APIRefreshGuestOsMetadataEvent.class
        }
    }
}