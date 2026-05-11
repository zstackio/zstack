package org.zstack.header.server

import org.zstack.header.server.APIDetachProvisionNetworkFromPoolEvent

doc {
    title "DetachProvisionNetworkFromPool"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/provision-networks/{networkUuid}/pools/{poolUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDetachProvisionNetworkFromPoolMsg.class

            desc """"""
            
			params {

				column {
					name "networkUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "poolUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APIDetachProvisionNetworkFromPoolEvent.class
        }
    }
}