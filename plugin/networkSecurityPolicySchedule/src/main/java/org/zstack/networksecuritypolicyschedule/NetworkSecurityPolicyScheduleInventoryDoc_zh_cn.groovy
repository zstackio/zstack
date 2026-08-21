package org.zstack.networksecuritypolicyschedule

import java.sql.Timestamp

doc {
    title "安全策略定时计划清单"

    field {
        name "uuid"
        desc "定时计划UUID"
        type "String"
        since "5.5.38"
    }
    field {
        name "name"
        desc "名称"
        type "String"
        since "5.5.38"
    }
    field {
        name "description"
        desc "描述"
        type "String"
        since "5.5.38"
    }
    field {
        name "resourceType"
        desc "所属资源类型"
        type "String"
        since "5.5.38"
    }
    field {
        name "resourceUuid"
        desc "所属资源UUID"
        type "String"
        since "5.5.38"
    }
    field {
        name "timeType"
        desc "时间类型，Local 或 UTC"
        type "String"
        since "5.5.38"
    }
    field {
        name "repeatType"
        desc "Once 或 Weekly"
        type "String"
        since "5.5.38"
    }
    field {
        name "startDate"
        desc "开始日期，格式yyyy-MM-dd"
        type "String"
        since "5.5.38"
    }
    field {
        name "endDate"
        desc "结束日期，格式yyyy-MM-dd"
        type "String"
        since "5.5.38"
    }
    field {
        name "startTime"
        desc "开始时间，格式HH:mm"
        type "String"
        since "5.5.38"
    }
    field {
        name "endTime"
        desc "结束时间，格式HH:mm"
        type "String"
        since "5.5.38"
    }
    field {
        name "weekDays"
        desc "Weekly生效星期"
        type "List"
        since "5.5.38"
    }
    field {
        name "effective"
        desc "当前分钟是否处于生效时间段"
        type "boolean"
        since "5.5.38"
    }
    field {
        name "expired"
        desc "从当前分钟起是否已不存在未来生效时间"
        type "boolean"
        since "5.5.38"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "5.5.38"
    }
    field {
        name "lastOpDate"
        desc "最后修改时间"
        type "Timestamp"
        since "5.5.38"
    }
}
