package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddIpRangeEvent

doc {
    title "添加IP地址范围(AddIpv6Range)"

    category "三层网络"

    desc """添加IPv6地址范围"""

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/ipv6-ranges"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddIpv6RangeMsg.class

            desc """"""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "3.1"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional false
					since "3.1"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "3.1"
					
				}
				column {
					name "startIp"
					enclosedIn "params"
					desc "起始地址"
					location "body"
					type "String"
					optional false
					since "3.1"
					
				}
				column {
					name "endIp"
					enclosedIn "params"
					desc "结束地址"
					location "body"
					type "String"
					optional false
					since "3.1"
					
				}
				column {
					name "gateway"
					enclosedIn "params"
					desc "网关"
					location "body"
					type "String"
					optional false
					since "3.1"
					
				}
				column {
					name "prefixLen"
					enclosedIn "params"
					desc "前缀长度"
					location "body"
					type "Integer"
					optional false
					since "3.1"
					
				}
				column {
					name "addressMode"
					enclosedIn "params"
					desc "IPv6地址分配模式"
					location "body"
					type "String"
					optional false
					since "3.1"
					values ("SLAAC","Stateful-DHCP","Stateless-DHCP")
				}
				column {
					name "ipRangeType"
					enclosedIn "params"
					desc "地址段类型"
					location "body"
					type "String"
					optional true
					since "3.9"
					values ("Normal","AddressPool")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，三层网络会使用该字段值作为UUID"
					location "body"
					type "String"
					optional true
					since "3.1"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.1"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1"
					
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIAddIpRangeEvent.class
        }
    }
}