package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIChangeL2NetworkVlanIdEvent

doc {
    title "ChangeL2NetworkVlanId"

    category "network.l2"

    desc """修改二层物理VlanId"""

    rest {
        request {
			url "PUT /v1/l2-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeL2NetworkVlanIdMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeL2NetworkVlanId"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "vlan"
					enclosedIn "changeL2NetworkVlanId"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.1.0"
				}
				column {
					name "type"
					enclosedIn "changeL2NetworkVlanId"
					desc ""
					location "body"
					type "String"
					optional true
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
            clz APIChangeL2NetworkVlanIdEvent.class
        }
    }
}