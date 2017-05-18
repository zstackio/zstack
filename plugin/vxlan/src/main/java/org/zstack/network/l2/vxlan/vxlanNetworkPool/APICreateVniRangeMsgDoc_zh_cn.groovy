package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APICreateVniRangeEvent

doc {
    title "CreateVniRange"

    category "network.l2"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/l2-networks/vxlan-pool/{l2NetworkUuid}/vni-ranges"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateVniRangeMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "startVni"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "0.6"
					
				}
				column {
					name "endVni"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "0.6"
					
				}
				column {
					name "l2NetworkUuid"
					enclosedIn "params"
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICreateVniRangeEvent.class
        }
    }
}