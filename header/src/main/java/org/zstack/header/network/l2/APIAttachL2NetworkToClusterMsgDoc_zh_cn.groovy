package org.zstack.header.network.l2



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIAttachL2NetworkToClusterMsg.class

            desc ""
            
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
            clz APIAttachL2NetworkToClusterEvent.class
        }
    }
}