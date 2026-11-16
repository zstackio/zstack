package org.zstack.physicalserver

import org.zstack.physicalserver.APIRestartPhysicalServerManagedServicesEvent

doc {
    title "重启物理服务器管控服务(RestartPhysicalServerManagedServices)"

    category "物理服务器"

    desc """为指定Role预写CPU和内存约束，重启Manifest中允许重启的服务，并在重启完成后异步同步资源分配。"""

    rest {
        request {
			url "PUT /v1/physical-servers/{serverUuid}/managed-services/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRestartPhysicalServerManagedServicesMsg.class

            desc """null"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn "restartPhysicalServerManagedServices"
					desc "物理服务器UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "roleType"
					enclosedIn "restartPhysicalServerManagedServices"
					desc "服务所属Role"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "serviceNames"
					enclosedIn "restartPhysicalServerManagedServices"
					desc "需要重启的稳定服务名；必须来自管控服务查询结果且restartable为true"
					location "body"
					type "List"
					optional false
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIRestartPhysicalServerManagedServicesEvent.class
        }
    }
}