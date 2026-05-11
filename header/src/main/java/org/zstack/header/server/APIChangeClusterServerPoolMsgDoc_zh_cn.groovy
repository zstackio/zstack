package org.zstack.header.server

import org.zstack.header.server.APIChangeClusterServerPoolEvent

doc {
    title "ChangeClusterServerPool"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/clusters/{clusterUuid}/server-pool/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeClusterServerPoolMsg.class

            desc """"""
            
			params {

				column {
					name "clusterUuid"
					enclosedIn "changeClusterServerPool"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "serverPoolUuid"
					enclosedIn "changeClusterServerPool"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APIChangeClusterServerPoolEvent.class
        }
    }
}