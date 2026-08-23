package org.zstack.physicalserver

import org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsEvent

doc {
    title "刷新物理服务器资源分配(RefreshPhysicalServerResourceAssignments)"

    category "物理服务器"

    desc """未指定serviceNames时，异步触发指定物理服务器全部Role的资源分配事实探测和收敛。指定roleType与serviceNames时，只重启Manifest中允许重启的目标服务；服务重启完成后触发资源分配重算。"""

    rest {
        request {
			url "PUT /v1/physical-servers/{serverUuid}/resource-assignments/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRefreshPhysicalServerResourceAssignmentsMsg.class

            desc """"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn "refreshPhysicalServerResourceAssignments"
					desc "物理服务器UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "roleType"
					enclosedIn "refreshPhysicalServerResourceAssignments"
					desc "需要重启服务的Role；仅与serviceNames同时填写"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "serviceNames"
					enclosedIn "refreshPhysicalServerResourceAssignments"
					desc "需要重启的稳定服务名；必须来自管控服务查询结果且restartable为true"
					location "body"
					type "List"
					optional true
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
            clz APIRefreshPhysicalServerResourceAssignmentsEvent.class
        }
    }
}