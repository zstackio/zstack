package org.zstack.header.vm

doc {
    title "CreateStopVmInstanceScheduler"

    category "vmInstance"

    desc "创建停止云主机的定时任务"

    rest {
        request {
            url "POST /v1/vm-instances/{vmUuid}/schedulers/stopping"


            header (Authorization: 'OAuth the-session-uuid')

            clz APICreateStopVmInstanceSchedulerMsg.class

            desc ""

            params {

                column {
                    name "vmUuid"
                    enclosedIn "params"
                    desc "云主机uuid"
                    location "url"
                    type "String"
                    optional false
                    since "0.6"

                }
                column {
                    name "schedulerName"
                    enclosedIn "params"
                    desc "定时任务名称"
                    location "body"
                    type "String"
                    optional false
                    since "1.6"

                }
                column {
                    name "schedulerDescription"
                    enclosedIn "params"
                    desc "定时任务描述"
                    location "body"
                    type "String"
                    optional true
                    since "1.6"

                }
                column {
                    name "type"
                    enclosedIn "params"
                    desc "定时任务类型，simple或者cron"
                    location "body"
                    type "String"
                    optional false
                    since "1.6"
                    values("simple", "cron")
                }
                column {
                    name "interval"
                    enclosedIn "params"
                    desc "定时任务间隔，单位秒"
                    location "body"
                    type "Integer"
                    optional true
                    since "1.6"

                }
                column {
                    name "repeatCount"
                    enclosedIn "params"
					desc "定时任务重复次数，仅针对simple类型的定时任务生效"
                    location "body"
                    type "Integer"
                    optional true
                    since "1.6"

                }
                column {
                    name "startTime"
                    enclosedIn "params"
                    desc "定时任务启动时间，必须遵循unix timestamp格式，0为从立刻开始"
                    location "body"
                    type "Long"
                    optional true
                    since "1.6"

                }
                column {
                    name "cron"
                    enclosedIn "params"
					desc "cron表达式，需遵循Java Quartz组件cron格式标准"
                    location "body"
                    type "String"
                    optional true
                    since "1.6"

                }
                column {
                    name "resourceUuid"
                    enclosedIn "params"
					desc "用户可指定创建Scheduler所使用的uuid"
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
            clz APICreateStopVmInstanceSchedulerEvent.class
        }
    }
}
