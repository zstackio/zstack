package org.zstack.header.vm

doc {
    title "GetCandidateIsoForAttachingVm"

    category "vmInstance"

    desc "在这里填写API描述"

    rest {
        request {
            url "GET /v1/vm-instances/{vmInstanceUuid}/iso-candidates"


            header(OAuth: 'the-session-uuid')

            clz APIGetCandidateIsoForAttachingVmMsg.class

            desc ""

            params {

                column {
                    name "vmInstanceUuid"
                    enclosedIn ""
                    desc "云主机UUID"
                    location "url"
                    type "String"
                    optional false
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
            clz APIGetCandidateIsoForAttachingVmReply.class
        }
    }
}