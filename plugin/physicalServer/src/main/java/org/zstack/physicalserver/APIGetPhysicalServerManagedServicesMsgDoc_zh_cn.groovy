package org.zstack.physicalserver

import org.zstack.physicalserver.APIGetPhysicalServerManagedServicesReply

doc {
    title "查看物理服务器管控服务资源使用(GetPhysicalServerManagedServices)"

    category "物理服务器"

    desc """返回物理服务器上各Role登记服务的运行事实。COMPUTE/MANAGEMENT由各自Manifest和Executor观测；ZBS由ZBS Agent只读返回三个固定Slice，并将其总体边界登记为只读Assignment。各Role独立观测：成功Role写入services，失败Role写入roleErrors，不影响其他Role返回。restartRequired表示服务已配置目标Role Slice但仍运行在旧cgroup中，需要经过允许的服务重启后生效；该字段实时探测且不落库。cpuSet为服务当前允许运行的CPU集合；cpuTime为cgroup累计CPU时间，单位纳秒；memory与memoryLimit单位均为字节，memoryLimit为包含父Role Slice约束后的有效上限，0表示不限。"""

    rest {
        request {
			url "GET /v1/physical-servers/{serverUuid}/managed-services"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetPhysicalServerManagedServicesMsg.class

            desc """查看指定物理服务器上各Role管控服务当前使用的CPU和内存资源。部分Role观测失败时API仍成功，并按Role返回标准错误码。"""
            
			params {

				column {
					name "serverUuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIGetPhysicalServerManagedServicesReply.class
        }
    }
}