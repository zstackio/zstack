package org.zstack.kvm.memory

doc {
    title "物理机内存节省量"
    field { name "hostUuid"; desc "物理机UUID。"; type "String"; since "5.5.48" }
    field { name "metrics"; desc "节省量指标：总节省及KSM、零页合并、ZRAM、磁盘转存各分项；内存量值单位为字节，没有可用样本时为空。metrics.metricQuality给出各分项（ksm_ordinary、ksm_zero、ksm_total、zram、total等）的数据质量：observed（有效值）、estimate（总节省为估算）、disabled（机制已关闭）、partial（不完整）、missing（未采样或采集失败）、epoch_changed（采样期间发生重建，本次样本不一致）、unsupported（内核不提供该计数，目前用于ksm_zero：零页合并仍会发生，但ksmZeroBytes为空、ksm_total为partial）。"; type "Map"; since "5.5.48" }
    field { name "quality"; desc "数据质量：Fresh、Partial、Stale或Unknown。"; type "String"; since "5.5.48" }
    field { name "sampleTime"; desc "样本时间，Unix epoch毫秒；没有样本时为空。"; type "Long"; since "5.5.48" }
}
