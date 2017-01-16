package org.zstack.header.volume

import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateEvent

doc {
    title "CreateDataVolumeFromVolumeTemplate"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/volumes/data/from/data-volume-templates/{imageUuid}"


            header (OAuth: 'the-session-uuid')

            clz APICreateDataVolumeFromVolumeTemplateMsg.class

            desc ""
            
			params {

				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
            clz APICreateDataVolumeFromVolumeTemplateEvent.class
        }
    }
}