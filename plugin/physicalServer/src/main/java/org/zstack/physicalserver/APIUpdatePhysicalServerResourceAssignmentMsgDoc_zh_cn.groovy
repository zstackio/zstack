package org.zstack.physicalserver

import org.zstack.physicalserver.APIUpdatePhysicalServerResourceAssignmentEvent

doc {
    title "更新物理服务器资源分配(UpdatePhysicalServerResourceAssignment)"

    category "物理服务器"

    desc """增量更新指定物理服务器上一个可控制Role的CPU和内存分配并异步触发同步。未填写的字段保持原值；同一物理服务器上的变更由服务端队列串行处理。v1可控制Role为COMPUTE和MANAGEMENT，其共享CPUSet不扣减计算容量。ZBS在v1提供只读Assignment和实时观测，不接受更新。"""

    rest {
        request {
			url "PUT /v1/physical-servers/{serverUuid}/resource-assignments/{roleType}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdatePhysicalServerResourceAssignmentMsg.class

            desc """"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn "updatePhysicalServerResourceAssignment"
					desc "物理服务器UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "roleType"
					enclosedIn "updatePhysicalServerResourceAssignment"
					desc "可控制节点Role：MANAGEMENT或COMPUTE"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "cpuSet"
					enclosedIn "updatePhysicalServerResourceAssignment"
					desc "CPU集合，例如0-3,8-11；未填写表示保持原值"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "memory"
					enclosedIn "updatePhysicalServerResourceAssignment"
					desc "Role总体内存上限，单位字节且必须为1MiB的整数倍；0表示不限，未填写表示保持原值。更新先持久化Role Slice与服务归属配置，不主动重启服务；运行中服务可调用托管服务重启API后进入该Slice"
					location "body"
					type "Long"
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
            clz APIUpdatePhysicalServerResourceAssignmentEvent.class
        }
    }
}
