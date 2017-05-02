package org.zstack.header.volume

doc {
    title "CreateVolumeSnapshot"

    category "volume"

    desc "从云盘创建快照"

    rest {
        request {
			url "POST /v1/volumes/{volumeUuid}/volume-snapshots"


            header (Authorization: 'OAuth the-session-uuid')

            clz APICreateVolumeSnapshotMsg.class

            desc ""
            
			params {

				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "快照名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "快照的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的Uuid"
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
            clz APICreateVolumeSnapshotEvent.class
        }
    }
}