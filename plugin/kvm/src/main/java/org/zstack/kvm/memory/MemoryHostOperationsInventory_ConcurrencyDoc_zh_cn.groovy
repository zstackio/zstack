package org.zstack.kvm.memory

doc {

	title "Host内存操作当前占用"

	field { name "activeOperations"; desc "当前占用普通并发额度的操作数，来源于服务字段C；不包括仅占用隔离队列额度的Q类操作，但隔离队列满后转而占用普通并发额度的超时操作计入此数。单位为操作数；缺失或无效观测不应解释为0。"; type "Integer"; since "5.5.38" }
	field { name "isolatedTimeoutOperations"; desc "当前单独占用超时隔离队列额度的操作数，来源于服务字段Q；不包括因隔离额度已满而占用普通并发额度的超时操作。单位为操作数；缺失或无效观测不应解释为0。"; type "Integer"; since "5.5.38" }
}
