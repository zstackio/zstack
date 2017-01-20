package org.zstack.header.host

import org.zstack.header.host.APIAddHostEvent

doc {
    title "AddHost"

    category "host"

    desc "添加物理机"

    rest {
        request {
			url "POST /v1/hosts"


            header (OAuth: 'the-session-uuid')

            clz APIAddHostMsg.class

            desc "添加物理机请求"
            
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
					name "managementIp"
					enclosedIn "params"
					desc "管理节点的IP"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的UUID"
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
            clz APIAddHostEvent.class
        }
    }
}