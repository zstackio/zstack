package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIAttachL2NetworkToClusterEvent

doc {
    title "挂载二层网络到集群(AttachL2NetworkToCluster)"

    category "二层网络"

    desc """挂载二层网络到集群"""

    rest {
        request {
			url "POST /v1/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIAttachL2NetworkToClusterMsg.class

            desc """"""
            
			params {

				column {
					name "l2NetworkUuid"
					enclosedIn ""
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
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
            clz APIAttachL2NetworkToClusterEvent.class
        }
    }
}