package org.zstack.physicalserver

import org.zstack.physicalserver.APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent

doc {
    title "从Role Profile刷新物理服务器资源分配(RefreshPhysicalServerResourceAssignmentsFromProfile)"

    category "物理服务器"

    desc """原子重载全部Role Profile，并将当前Profile中的服务与cgroup配置作为完整期望配置，结合Assignment中的CPU、内存边界覆盖到指定物理服务器；不指定serverUuids时处理全部Assignment。实际覆盖异步执行，该API不重新发现Role关系，也不重启服务。"""

    rest {
        request {
			url "PUT /v1/physical-servers/resource-assignments/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRefreshPhysicalServerResourceAssignmentsFromProfileMsg.class

            desc """"""
            
			params {

				column {
					name "serverUuids"
					enclosedIn "refreshPhysicalServerResourceAssignmentsFromProfile"
					desc "可选的物理服务器UUID列表；不传表示从当前Profile刷新全部Assignment，显式空列表非法"
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
            clz APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent.class
        }
    }
}