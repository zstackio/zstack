package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIDeleteVolumeSnapshotGroupEvent

doc {
    title "DeleteVolumeSnapshotGroup"

    category "snapshot.volume"

    desc """删除快照组"""

    rest {
        request {
			url "DELETE /v1/volume-snapshots/group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteVolumeSnapshotGroupMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
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
			}
        }

        response {
            clz APIDeleteVolumeSnapshotGroupEvent.class
        }
    }
}