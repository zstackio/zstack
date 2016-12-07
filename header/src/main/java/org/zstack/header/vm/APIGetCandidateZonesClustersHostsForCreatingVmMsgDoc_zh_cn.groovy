package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vm-instances/candidate-destinations"


            header (OAuth: 'the-session-uuid')

            clz APIGetCandidateZonesClustersHostsForCreatingVmMsg.class

            desc ""
            
			params {

				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "query"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "query"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn "params"
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetCandidateZonesClustersHostsForCreatingVmReply.class
        }
    }
}