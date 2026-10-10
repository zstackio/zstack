package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetHostMemoryWritebackBackendsReply

doc {
    title "GetHostMemoryWritebackBackends"

    category "memoryOptimization"

    desc """列出指定Host上经过资格检查的写回后端设备及准备状态；不接受任意路径作为授权。"""

    rest {
        request {
			url "GET /v1/hosts/{hostUuid}/memory-writeback-backends"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetHostMemoryWritebackBackendsMsg.class

            desc """设备容量字段使用字节。该只读结果用于选择和预览；实际准备仍需专用证据及再次校验。"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
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
            clz APIGetHostMemoryWritebackBackendsReply.class
        }
    }
}