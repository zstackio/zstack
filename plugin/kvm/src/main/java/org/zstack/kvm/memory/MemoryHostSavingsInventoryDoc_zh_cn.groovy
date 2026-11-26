package org.zstack.kvm.memory

doc {
    title "物理机内存节省量"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "metrics"; desc "节省量指标：总节省及KSM、零页合并、ZRAM、磁盘转存各分项；内存量值单位为字节，没有可用样本时为空。"; type "Map"; since "5.5.48" }
    field { name "hotMemoryServiceState"; desc "最近一次有效主机观测中的热内存采样服务状态。absent 表示未安装，unknown 表示读取失败。旧版本未上报或观测过期时为空。"; type "String"; since "5.5.48" }
    field { name "quality"; desc "数据质量：Fresh、Partial、Stale或Unknown。"; type "String"; since "5.5.48" }
    field { name "sampleTime"; desc "样本时间，Unix epoch毫秒；没有样本时为空。"; type "Long"; since "5.5.48" }
}
