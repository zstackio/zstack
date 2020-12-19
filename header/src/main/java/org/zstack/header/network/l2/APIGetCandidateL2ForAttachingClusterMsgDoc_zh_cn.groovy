package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIGetCandidateL2ForAttachingClusterReply

doc {
    title "获取集群允许加载的二层网络"

    category "二层网络"

    desc """获取集群允许加载的二层网络"""

    rest {
        request {
			url "GET /v1/cluster/{clusterUuid}/l2-candidates"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetCandidateL2ForAttachingClusterMsg.class

            desc """"""
            
			params {

				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "4.0.0"
					
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "4.0.0"
					
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "4.0.0"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.0.0"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.0.0"
					
				}
			}
        }

        response {
            clz APIGetCandidateL2ForAttachingClusterReply.class
        }
    }
}