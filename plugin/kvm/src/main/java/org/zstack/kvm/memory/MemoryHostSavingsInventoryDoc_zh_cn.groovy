package org.zstack.kvm.memory

doc {
    title "物理机内存节省量"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "metrics"; desc "节省量指标，与GetMemoryStates返回的metrics相同；内存量值单位为字节，没有可用样本时为空。"; type "Map"; since "5.5.48" }
    field { name "quality"; desc "数据质量：Fresh、Partial、Stale或Unknown，规则与GetMemoryStates相同。"; type "String"; since "5.5.48" }
    field { name "sampleTime"; desc "样本时间，Unix epoch毫秒；没有样本时为空。"; type "Long"; since "5.5.48" }
}
