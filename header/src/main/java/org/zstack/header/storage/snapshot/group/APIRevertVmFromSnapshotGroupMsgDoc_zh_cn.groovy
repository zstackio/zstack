package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIRevertVmFromSnapshotGroupEvent

doc {
    title "RevertVmFromSnapshotGroup"

    category "snapshot.volume"

    desc """从快照组恢复云主机"""

    rest {
        request {
			url "PUT /v1/volume-snapshots/group/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRevertVmFromSnapshotGroupMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "revertVmFromSnapshotGroup"
					desc "快照组的UUID"
					location "url"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "startVm"
					enclosedIn "revertVmFromSnapshotGroup"
					desc "普通快照组恢复成功后是否开机。未传入时，恢复前为 Running 默认 true，为 Stopped 默认 false；含内存快照的组恢复成功后始终为 Running，本参数不改变该行为。"
					location "body"
					type "Boolean"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIRevertVmFromSnapshotGroupEvent.class
        }
    }
}