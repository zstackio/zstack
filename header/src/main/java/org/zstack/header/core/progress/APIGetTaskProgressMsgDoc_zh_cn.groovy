package org.zstack.header.core.progress

doc {
    title "获取任务进度(GetTaskProgress)"

    category "core.progress"

    desc "获取任务进度"

    rest {
        request {
			url "GET /v1/progress/{procesType}/{resourceUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIGetTaskProgressMsg.class

            desc ""
            
			params {

				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "该任务对应的资源Uuid"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "processType"
					enclosedIn "params"
					desc "任务类型"
					location "url"
					type "String"
					optional true
					since "0.6"
					values ("AddImage","LocalStorageMigrateVolume","CreateRootVolumeTemplateFromRootVolume")
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetTaskProgressReply.class
        }
    }
}