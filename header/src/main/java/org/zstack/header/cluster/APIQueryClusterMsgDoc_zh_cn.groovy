package org.zstack.header.cluster

org.zstack.header.cluster.APIQueryClusterReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryCluster"

    category "cluster"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/clusters"

			url "GET /v1/clusters/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryClusterMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryClusterReply.class
        }
    }
}