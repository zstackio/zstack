package org.zstack.header.cluster

doc {
    title "DeleteCluster"

    category "cluster"

    desc "管理员可以使用DeleteCluster命令来删除一个集群"

    rest {
        request {
			url "DELETE /v1/clusters/{uuid}"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteClusterMsg.class

            desc "管理员可以使用DeleteCluster命令来删除一个集群"
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn "params"
					desc "删除模式"
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
            clz APIDeleteClusterEvent.class
        }
    }
}
