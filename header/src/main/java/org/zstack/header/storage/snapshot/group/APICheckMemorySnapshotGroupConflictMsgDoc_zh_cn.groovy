package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply

doc {
    title "检测内存快照组网卡冲突(CheckMemorySnapshotGroupConflict)"

    category "snapshot.volume"

    desc """检查内存快照组网卡存档中的 IP、MAC 是否与其他云主机的现有网卡重复，检测不按三层网络限定。

需要具有目标快照组的访问权限。冲突云主机不可见时，仍返回 IP、MAC 冲突，但隐藏其 UUID、名称和网卡内部名称。

vmNicConflict 返回冲突网卡列表；没有检测到冲突时为空列表。结果仅反映本次预检，不保证恢复时无冲突或网络连通。"""

    rest {
        request {
			url "GET /v1/memory-snapshots/groups/{uuid}/conflict-detection"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICheckMemorySnapshotGroupConflictMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "待检测的内存快照组 UUID"
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
            clz APICheckMemorySnapshotGroupConflictReply.class
        }
    }
}