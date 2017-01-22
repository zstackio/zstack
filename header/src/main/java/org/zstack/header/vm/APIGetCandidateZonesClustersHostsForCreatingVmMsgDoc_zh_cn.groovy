package org.zstack.header.vm

doc {
    title "获取可创建云主机的目的地列表(GetCandidateZonesClustersHostsForCreatingVm)"

    category "vmInstance"

    desc "获取可以创建指定云主机参数的目的区域、集群、物理机。用户可以使用该API，通过指定云主机参数获得可以创建满足参数云主机的目的地。"

    rest {
        request {
            url "GET /v1/vm-instances/candidate-destinations"


            header(OAuth: 'the-session-uuid')

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
                    desc "三层网络列表"
                    location "query"
                    type "List"
                    optional false
                    since "0.6"

                }
                column {
                    name "rootDiskOfferingUuid"
                    enclosedIn "params"
                    desc "根磁盘规格。仅在`imageUuid`指定的镜像是ISO时需要指定"
                    location "query"
                    type "String"
                    optional true
                    since "0.6"

                }
                column {
                    name "dataDiskOfferingUuids"
                    enclosedIn "params"
                    desc "云盘规格列表"
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
                    desc "默认三层网络UUID"
                    location "query"
                    type "String"
                    optional true
                    since "0.6"

                }
                column {
                    name "systemTags"
                    enclosedIn ""
                    desc "系统标签"
                    location "query"
                    type "List"
                    optional true
                    since "0.6"

                }
                column {
                    name "userTags"
                    enclosedIn ""
                    desc "用户标签"
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