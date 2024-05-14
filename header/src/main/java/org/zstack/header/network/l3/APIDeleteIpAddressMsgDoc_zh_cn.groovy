package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIDeleteIpAddressEvent

doc {
    title "DeleteIpAddress"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/ip-address"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteIpAddressMsg.class

            desc """"""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "usedIpUuids"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional false
					since "5.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
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
            clz APIDeleteIpAddressEvent.class
        }
    }
}