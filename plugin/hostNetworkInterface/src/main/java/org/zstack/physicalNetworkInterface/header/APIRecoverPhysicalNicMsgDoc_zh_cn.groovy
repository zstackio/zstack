package org.zstack.physicalNetworkInterface.header

import org.zstack.physicalNetworkInterface.header.APIRecoverPhysicalNicEvent

doc {
    title "RecoverPhysicalNic"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/physical-nic/{physicalNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRecoverPhysicalNicMsg.class

            desc """"""
            
			params {

				column {
					name "physicalNicUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"

				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"

				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "pciDeviceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "deleteMode"
					enclosedIn "params"
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "0.6"

				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"

				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"

				}
			}
        }

        response {
            clz APIRecoverPhysicalNicEvent.class
        }
    }
}