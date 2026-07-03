package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIChangeVolumeProtocolEvent

doc {
    title "ChangeVolumeProtocol"

    category "volume"

    desc """修改云盘的数据面协议"""

    rest {
        request {
			url "PUT /v1/volumes/{volumeUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeVolumeProtocolMsg.class

            desc """"""
            
			params {

				column {
					name "volumeUuid"
					enclosedIn "changeVolumeProtocol"
					desc "云盘的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.28"
				}
				column {
					name "protocol"
					enclosedIn "changeVolumeProtocol"
					desc "目标数据面协议"
					location "body"
					type "String"
					optional false
					since "5.5.28"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.28"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.28"
				}
			}
        }

        response {
            clz APIChangeVolumeProtocolEvent.class
        }
    }
}