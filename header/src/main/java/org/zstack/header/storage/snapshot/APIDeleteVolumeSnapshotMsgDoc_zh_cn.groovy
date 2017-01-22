package org.zstack.header.storage.snapshot

doc {
    title "删除云盘快照(DeleteVolumeSnapshot)"

    category "snapshot.volume"

    desc "删除云盘快照"

    rest {
        request {
			url "DELETE /v1/volume-snapshots/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDeleteVolumeSnapshotMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
 					enclosedIn "params"
 					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
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