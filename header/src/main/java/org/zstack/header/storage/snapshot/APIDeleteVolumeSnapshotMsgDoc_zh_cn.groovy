package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotEvent

doc {
    title "删除云盘快照(DeleteVolumeSnapshot)"

    category "snapshot.volume"

    desc """删除云盘快照"""

    rest {
        request {
			url "DELETE /v1/volume-snapshots/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIDeleteVolumeSnapshotMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
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
            clz APIDeleteVolumeSnapshotEvent.class
        }
    }
}