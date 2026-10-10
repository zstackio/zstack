package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIUpdateMemoryPolicyEvent

doc {
    title "UpdateMemoryPolicy"

    category "memoryOptimization"

    desc """以revision和幂等请求UUID为条件提交策略或显式维护动作；异步响应只表示任务已接受，不表示Host执行成功。"""

    rest {
        request {
			url "PUT /v1/memory-policies/{resourceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateMemoryPolicyMsg.class

            desc """请求体使用 `updateMemoryPolicy` action wrapper；`policy` 是JSON字符串。普通 apply 应先读取当前 revision/sourceRevisions。"""

			params {

				column {
					name "recovery"
					enclosedIn "updateMemoryPolicy"
					desc "不确定结果恢复证据对象；只用于 recoverUncertain。对象字段：expectedHostBootId (String，当前Host启动标识，必须匹配最新观测)；expectedPoolGeneration (String，待恢复旧池代次，64位小写十六进制摘要)；drainControlOperationUuid (String，已成功排空该Host/池代次的操作UUID，32位小写十六进制)；confirmed (boolean，操作者已核实恢复前置条件，必须为true)。恢复不会伪造原Unknown任务结果。"
					location "body"
					type "MemoryUncertainRecovery"
					optional true
					since "5.5.38"
				}
				column {
					name "backendPreparation"
					enclosedIn "updateMemoryPolicy"
					desc "写回设备准备证据对象；仅用于 prepareWritebackBackend。对象字段：candidateId (String，候选查询返回的candidate:加64位小写十六进制摘要，不是设备路径)；expectedHostBootId (String，候选查询时的Host启动标识，提交时须仍匹配)；expectedPoolGeneration (String，候选查询时池代次，无池为none，否则为64位小写十六进制摘要)；backendCapacityBytes (long，候选确认的容量，字节，须为4096整数倍且在接口范围内)；resetConfirmed (boolean，确认候选设备可供写回且现有内容可重置，必须为true)。"
					location "body"
					type "MemoryBackendPreparation"
					optional true
					since "5.5.38"
				}
				column {
					name "poolPreparation"
					enclosedIn "updateMemoryPolicy"
					desc "ZRAM池准备证据对象；仅用于 prepareZramPool。对象字段：expectedHostBootId (String，读取池状态时的Host启动标识，提交时须仍匹配)；expectedPoolGeneration (String，已排空且准备重置的旧池代次，64位小写十六进制摘要)；resetConfirmed (boolean，仅在池状态及代次校验通过后确认重置，必须为true)。"
					location "body"
					type "MemoryZramPoolPreparation"
					optional true
					since "5.5.38"
				}
				column {
					name "expectedSourceRevisions"
					enclosedIn "updateMemoryPolicy"
					desc "apply、clearOverride及准备动作回传最近Get/Preview返回的完整sourceRevisions映射；其他受控动作传入时也校验，stageTargetShard/cancelTargetShards禁止传入。键包含各实际来源层级及资源UUID，不得自行截取或补版本。"
					location "body"
					type "Map"
					optional true
					since "5.5.38"
				}
				column {
					name "clearOverrideFields"
					enclosedIn "updateMemoryPolicy"
					desc "清除的字段覆盖名列表；仅用于 clearOverride。"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "expectedGlobalRevision"
					enclosedIn "updateMemoryPolicy"
					desc "旧Host/Cluster客户端的兼容全局来源检查；同时提供完整expectedSourceRevisions时两者都须匹配。stageTargetShard/cancelTargetShards禁止传入。新调用应回传完整sourceRevisions。"
					location "body"
					type "Long"
					optional true
					since "5.5.38"
				}
				column {
					name "expectedInstanceGeneration"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于VM资源的操作，其他层级禁止传入；运行中VM操作须提供当前实例代次，避免作用于已更换实例。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "targetHostUuids"
					enclosedIn "updateMemoryPolicy"
					desc "本专用入口中Global/Cluster执行动作须显式选择非空且属于该层级的KVM Host；Host/VM执行动作禁止传入。stageTargetShard仅为旧客户端兼容时允许此字段代替targetVmUuids承载VM分片，二者不能同时提供；新调用须用targetVmUuids。cancelTargetShards禁止传入。标准配置API不使用该参数。"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "scope"
					enclosedIn "updateMemoryPolicy"
					desc "仅旧客户端兼容字段；新客户端省略。由resourceUuid对应的真实资源推导层级，global标识全局；显式传入时必须与资源层级一致。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
					values ("Global","Cluster","Host","VM")
				}
				column {
					name "resourceUuid"
					enclosedIn "updateMemoryPolicy"
					desc "资源UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "action"
					enclosedIn "updateMemoryPolicy"
					desc "动作：apply、clearOverride、pause、drain、resume、reconcile、recoverUncertain及后端/池/分片准备或提交。"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("apply","clearOverride","pause","drain","resume","reconcile","recoverUncertain","prepareWritebackBackend","prepareZramPool","stageTargetShard","commitTargetShards","cancelTargetShards")
				}
				column {
					name "expectedRevision"
					enclosedIn "updateMemoryPolicy"
					desc "客户端读取到的当前策略revision；执行提交时不匹配则冲突。暂存分片将该版本绑定到快照，commit时重新核对；取消暂存时必须与该批分片绑定版本相同。"
					location "body"
					type "long"
					optional false
					since "5.5.38"
				}
				column {
					name "expectedControlOperationUuid"
					enclosedIn "updateMemoryPolicy"
					desc "预期Host控制操作UUID；受控动作的fencing凭据，参与请求幂等身份。分片暂存/提交/取消禁止传入。恢复、协调及准备动作按各自回执合同核对；其他Host动作提供时也必须匹配当前控制操作。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "policy"
					enclosedIn "updateMemoryPolicy"
					desc "策略JSON字符串；容量字段单位为字节，时间/阈值单位按各策略字段定义。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "clientRequestUuid"
					enclosedIn "updateMemoryPolicy"
					desc "客户端为一次逻辑请求生成的UUID；对同一请求重试时必须复用。"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "targetSnapshotGeneration"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard/commitTargetShards的目标VM快照代次；其他动作禁止传入。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "targetShardIndex"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard的当前分片索引，从0开始；commit读取已暂存分片，禁止再次传索引。"
					location "body"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "targetShardCount"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard/commitTargetShards的分片总数。"
					location "body"
					type "Integer"
					optional true
					since "5.5.38"
				}
				column {
					name "targetTotalCount"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard/commitTargetShards的完整目标VM总数。"
					location "body"
					type "Long"
					optional true
					since "5.5.38"
				}
				column {
					name "targetDigest"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard/commitTargetShards的完整目标VM集合摘要；commit按已暂存内容重新核对。"
					location "body"
					type "String"
					optional true
					since "5.5.38"
				}
				column {
					name "targetVmUuids"
					enclosedIn "updateMemoryPolicy"
					desc "仅用于stageTargetShard的VM UUID分片列表；不能与旧兼容targetHostUuids同时提供。commit只消费暂存内容，其他动作禁止传此字段。"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APIUpdateMemoryPolicyEvent.class
        }
    }
}
