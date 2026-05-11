package org.zstack.header.server

import org.zstack.header.server.APIDeleteProvisionNetworkEvent

doc {
    title "DeleteProvisionNetwork"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/provision-networks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteProvisionNetworkMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
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
            clz APIDeleteProvisionNetworkEvent.class
        }
    }
}