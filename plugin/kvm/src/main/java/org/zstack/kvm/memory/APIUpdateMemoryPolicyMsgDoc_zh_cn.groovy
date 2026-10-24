package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIUpdateMemoryPolicyEvent

doc {
    title "UpdateMemoryPolicy"

    category "memoryOptimization"

    desc """以revision和幂等请求UUID为条件提交显式维护动作。HTTP 202仅表示REST异步job已受理，须按location轮询原job；最终API成功并返回MemoryTask inventory才表示维护任务已接受，仍不表示Host执行成功。普通GlobalConfig/ResourceConfig保存不调用本接口，也不因Host为Unknown而要求前端先恢复或排空；后端仅在原回执及独立当前证明完整时自动收尾并应用最新保存配置，缺证保留Unknown。writeback开关可先于设备配置或准备独立保存，生效条件由后端检查，不能以候选未ready禁用普通保存。选盘仅提交一次prepareWritebackBackend，浏览器只需准备权限及整盘确认，不串联drain/reconcile。已准备后端但尚无池时，更换另一未使用设备仍走此请求，不要求先开启ZRAM或删除历史；后端须核完整准备回执、当前归属/配置与真实无池无数据，按原维护流程保存旧设备证据，不凭APPLIED或空代次放行。BFF仍须读取同Host的States控制身份、Policy版本及Backends候选证据；这些标识不是用户填写项。候选writebackReady不等于绑定及整项维护成功，不能据此提前报成功或重复格式化；BFF若等待任务完成，应核对返回root任务的聚合结果，不能只等prepare子任务。安全准入失败返回明确错误，任务受理后从States.status展示结果。"""

    rest {
        request {
			url "PUT /v1/memory-policies/{resourceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateMemoryPolicyMsg.class

            desc """请求体使用 `updateMemoryPolicy` action wrapper。普通配置使用 GlobalConfig/ResourceConfig；本接口只提交受控动作。选盘使用 prepareWritebackBackend 一次提交，省略可选兼容 scope 和无配置用途的 policy；后端自动维护，前端仅从 States.status 展示结果。"""

			params {

				column {
					name "recovery"
					enclosedIn "updateMemoryPolicy"
						desc "不确定结果恢复证据对象；只用于 recoverUncertain。对象字段：expectedHostBootId (String，当前Host启动标识，必须匹配最新观测)；expectedPoolGeneration (String，当前待恢复池代次，64位小写十六进制摘要)；drainControlOperationUuid (String，激活/退役恢复必填，已成功排空该Host/池代次的操作UUID；仅调参/CPU部分写入的受控恢复时省略，不得填null)；confirmed (boolean，操作者已核实恢复前置条件，必须为true)。后端只接受原Unknown操作及同Host/启动/控制/修订的完整证据。正常情况下不重放已确认步骤；若原操作的KSM与CPU保持步骤已有完整回执，且调参请求被执行器静止门以精确同步错误确认在写入前拒绝，则可基于原成功调参锚点，以同一请求身份和负载补齐该调参，再核读回并恢复CPU门。对于该部分恢复，缺少可信锚点或当前读回时保持恢复结果Unknown且不发剩余写入；超时或写入回执有歧义时也保持Unknown。独立的API参数校验或准入拒绝可使新恢复任务以Failed结束，原任务仍保持Unknown。恢复任务成功不改写原任务的历史Unknown结果。"
					location "body"
					type "MemoryUncertainRecovery"
					optional true
					since "5.5.38"
				}
				column {
					name "backendPreparation"
					enclosedIn "updateMemoryPolicy"
					desc "写回设备自动维护证据对象；仅用于 Host prepareWritebackBackend。对象字段：candidateId (String，候选查询返回的candidate:加64位小写十六进制摘要，不是设备路径)；expectedHostBootId (String，候选查询时的Host启动标识，提交时须仍匹配)；expectedPoolGeneration (String，候选查询时池代次，无池为none，否则为64位小写十六进制摘要)；backendCapacityBytes (long，候选实测整设备容量，字节，非任意使用配额，须为4096整数倍且在接口范围内)；resetConfirmed (boolean，明确确认所选设备现有内容可重置，必须为true)。后端校验并按需排空、准备设备、原子保存可信设备标识与容量，再恢复原策略；不隐式开启原关闭的功能。首次无池且仅服务未停时，核无数据、无未决及服务身份后自动正常退出、准备及恢复；超时不强杀，不改KSM或VM。前端无需串联维护、手动停服务或补写设备UUID，仅查询States.status展示结果。canPrepare描述可否直接离线准备，不能单凭其false推断自动维护一定不可受理，后端仍逐项核验拒绝原因及适用的首次使用或排空条件。"
					location "body"
					type "MemoryBackendPreparation"
					optional true
					since "5.5.38"
				}
				column {
					name "expectedSourceRevisions"
					enclosedIn "updateMemoryPolicy"
					desc "准备动作回传最近Get返回的完整sourceRevisions映射；其他受控动作传入时也校验。键包含各实际来源层级及资源UUID，不得自行截取或补版本。"
					location "body"
					type "Map"
					optional true
					since "5.5.38"
				}
				column {
					name "expectedGlobalRevision"
					enclosedIn "updateMemoryPolicy"
					desc "旧Host/Cluster客户端的兼容全局来源检查；同时提供完整expectedSourceRevisions时两者都须匹配。新调用应回传完整sourceRevisions。"
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
					desc "本专用入口中Global/Cluster执行动作须显式选择非空且属于该层级的KVM Host；Host/VM执行动作禁止传入。标准配置API不使用该参数。"
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
					desc "动作：pause、drain、resume、reconcile、recoverUncertain及后端准备。resume 也用于排空后的恢复：同一启动周期内由后端自动重置已排空的ZRAM池再激活；Host重启后设备已为空，直接按当前配置重新下发。reconcile用于核对既有结果；对证据完整的精确写前拒绝，可持久记录NOT_ISSUED并将原Unknown结案为Failed、释放活动占用，不会标记成功或重放原请求。原配置仍对应当前计划及期望修订时，该计划及States.status同步为Failed；旧结果不覆盖更新的计划。实际已应用修订、原生观测及安全准入不变，功能状态和监控质量独立判断；证据不足仍为Unknown。纯KSM的none目标在Host重启后重新核验本次Host/启动身份、受管配置和完整原生读数，保持当前开关，不复用旧启动ACK；其精确写前拒绝仅封存本次失败，不要求ZRAM服务，也不修改历史成功记录。"
					location "body"
					type "String"
					optional false
					since "5.5.38"
					values ("pause","drain","resume","reconcile","recoverUncertain","prepareWritebackBackend")
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
					desc "预期Host控制操作UUID；受控动作的fencing凭据，参与请求幂等身份。prepareWritebackBackend取同一Host的States.controlOperationUuid（管理端控制身份），不得用写回候选清单中Agent观测的同名字段代填。同Host状态记录无控制UUID时，首次准备沿原合同省略本参数，不伪造控制身份；查不到记录或读取失败不能当作无控制。分片暂存/提交/取消禁止传入。恢复、协调及其他准备动作按各自回执合同核对；其他Host动作提供时也必须匹配当前控制操作。"
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
