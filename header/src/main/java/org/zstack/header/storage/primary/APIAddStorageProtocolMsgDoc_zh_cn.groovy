package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIAddStorageProtocolEvent

doc {
    title "AddStorageProtocol"

    category "storage.primary"

    desc """添加存储协议"""

    rest {
        request {
			url "POST /v1/primary-storage/protocol"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddStorageProtocolMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "outputProtocol"
					enclosedIn ""
					desc "输出协议"
					location "body"
					type "String"
					optional false
					since "5.1.0"
				}
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
            clz APIAddStorageProtocolEvent.class
        }
    }
}