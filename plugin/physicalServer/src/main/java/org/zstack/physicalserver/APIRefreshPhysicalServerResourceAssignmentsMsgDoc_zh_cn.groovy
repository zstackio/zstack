package org.zstack.physicalserver

import org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsEvent

doc {
    title "刷新物理服务器资源分配(RefreshPhysicalServerResourceAssignments)"

    category "物理服务器"

    desc """异步重新发现指定物理服务器的Role关联，并同步对应资源分配。该API不重启服务。"""

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
