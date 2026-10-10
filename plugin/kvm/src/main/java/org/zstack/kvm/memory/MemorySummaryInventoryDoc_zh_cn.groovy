package org.zstack.kvm.memory

import java.lang.Long

doc {

	title "内存优化汇总"

	field {
		name "formulaVersion"
		desc "节省量计算公式版本。"
		type "String"
		since "5.5.38"
	}
	field {
		name "sampleTime"
		desc "样本时间，Unix epoch毫秒。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "totalSavedEstimateBytes"
		desc "KSM与ZRAM机制估算的总节省量，单位字节；有效负值不截断。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "ksmOrdinaryBytes"
		desc "普通KSM节省量，单位字节。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "ksmZeroBytes"
		desc "零页KSM节省量，单位字节。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "ksmTotalBytes"
		desc "KSM总节省量，单位字节。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "zramBytes"
		desc "ZRAM总节省估算，单位字节。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "zramNormalBytes"
		desc "常驻、非写回ZRAM节省量，单位字节。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "zramWritebackBytes"
		desc "当前写回逻辑数据量，单位字节；不是累计写入IO。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "expectedHosts"
		desc "本次汇总范围内预期Host数。"
		type "int"
		since "5.5.38"
	}
	field {
		name "coveredHosts"
		desc "具备有效指标覆盖的Host数。"
		type "int"
		since "5.5.38"
	}
	field {
		name "quality"
		desc "汇总质量和覆盖状态。"
		type "String"
		since "5.5.38"
	}
	field {
		name "metricCoverage"
		desc "各指标覆盖质量细节。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "currentStatus"
		desc "新鲜状态样本中各状态的Host数量；不从过期样本推断。"
		type "Map"
		since "5.5.38"
	}
	field {
		name "currentStatusSampleTime"
		desc "状态计数样本时间，Unix epoch毫秒。"
		type "Long"
		since "5.5.38"
	}
	field {
		name "currentStatusTtlMillis"
		desc "状态样本有效期，单位毫秒。"
		type "Long"
		since "5.5.38"
	}
}
