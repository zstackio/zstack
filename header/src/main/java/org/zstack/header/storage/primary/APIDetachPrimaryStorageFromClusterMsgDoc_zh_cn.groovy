package org.zstack.header.storage.primary

doc {
    title "从集群卸载主存储(DetachPrimaryStorageFromCluster)"

    category "storage.primary"

    desc "从集群卸载主存储"

    rest {
        request {
			url "DELETE /v1/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDetachPrimaryStorageFromClusterMsg.class

            desc ""
            
			params {

				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDetachPrimaryStorageFromClusterEvent.class
        }
    }
}