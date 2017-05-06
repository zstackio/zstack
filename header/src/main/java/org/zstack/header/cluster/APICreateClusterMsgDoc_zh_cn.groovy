package org.zstack.header.cluster

import org.zstack.header.cluster.APICreateClusterEvent

doc {
    title "CreateCluster"

    category "cluster"

    desc """管理员可以使用CreateCluster命令来创建一个集群"""

    rest {
        request {
			url "POST /v1/clusters"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICreateClusterMsg.class

            desc """管理员可以使用CreateCluster命令来创建一个集群"""
            
			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
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
					name "hypervisorType"
					enclosedIn "params"
					desc "虚拟机管理程序类型"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("KVM","Simulator")
				}
				column {
					name "type"
					enclosedIn "params"
					desc "保留域, 请不要使用"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("zstack")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "当reourceUuid不等于null, ZStack使用这个值作为被创建资源的UUID; 否则ZStack会自动生成一个UUID"
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
            clz APICreateClusterEvent.class
        }
    }
}