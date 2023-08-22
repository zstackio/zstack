package org.zstack.network.service.vip

import org.zstack.network.service.vip.APIGetVipFreePortReply

doc {
    title "GetVipFreePort"

    category "vip"

    desc """获取VIP空闲端口"""

    rest {
        request {
			url "GET /v1/vips/{vipUuid}/get-vip-free-port"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetVipFreePortMsg.class

            desc """"""
            
			params {

				column {
					name "vipUuid"
					enclosedIn ""
					desc "VIP UUID"
					location "url"
					type "String"
					optional false
					since "4.7.11"
				}
				column {
					name "protocolType"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "4.7.11"
					values ("TCP","UDP")
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "4.7.11"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "4.7.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.7.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.7.11"
				}
			}
        }

        response {
            clz APIGetVipFreePortReply.class
        }
    }
}