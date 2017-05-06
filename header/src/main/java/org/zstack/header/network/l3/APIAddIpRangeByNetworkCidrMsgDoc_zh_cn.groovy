package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddIpRangeByNetworkCidrEvent

doc {
    title "通过网络CIDR添加IP地址范围(AddIpRangeByNetworkCidr)"

    category "三层网络"

    desc """通过网络CIDR添加IP地址范围"""

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/ip-ranges/by-cidr"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIAddIpRangeByNetworkCidrMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "networkCidr"
					enclosedIn "params"
					desc "网络CIDR"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，三层网络会使用该字段值作为UUID"
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
            clz APIAddIpRangeByNetworkCidrEvent.class
        }
    }
}