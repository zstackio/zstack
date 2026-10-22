# ZSTAC-89798 网卡后端与型号规则（5.6.0）

本单接续已合并的 ZSTAC-73593。原提交解除 Other/FreeBSD 单网卡型号修改限制；本单负责后端兼容性与生命周期一致性，不增加 FreeBSD 特殊分支，不涉及 zwatch 检查或 FreeBSD QGA 网络配置。

## 两个相互独立的配置

`VmNicVO.type` 决定设备后端，`driverType` 决定客户机看到的虚拟网卡型号。FreeBSD、Linux、Windows 均使用同一套规则。允许配置某型号不等于客户机已经安装驱动，也不等于所有架构/QEMU/libvirt 版本均支持它。

| 输入 | 来源与入口 | 现有行为及本次处理 |
| --- | --- | --- |
| 镜像 virtio | `VmInstanceManagerImpl.instantiateTagsForCreateMessage` | 普通镜像路径读取 ImageVO.virtio，真值创建 VM `driver::virtio` 标签；false 不删除请求已经携带的标签。 |
| 根盘/磁盘创建 | 同上，diskAOs 非空 | 使用内部 CreateVmInstanceMsg.virtio；不能把此路径描述为读取镜像开关。 |
| 请求与继承标签 | 同上 | 先创建请求标签，再按上述来源启用 virtio，之后复制规格/镜像标签；网卡分配读取最终 VM 标签。保留既有继承语义。 |
| 显式型号 | VmNicSpec.nicDriverType | 非空优先于默认值和插件，但不能绕过后端硬约束。 |
| 插件默认偏好 | VmNicSetDriverExtensionPoint | 复用 PluginRegistry 的 order（越大越优先）。最高有效优先级有不同非空结果时拒绝，避免相同优先级受注册顺序影响；相同结果允许。动态扩展无 order 时按 0。 |
| 平台默认 | ImagePlatform.isParaVirtualization | Linux、WindowsVirtio、Paravirtualization 为真；Windows、Other 为假。没有插件偏好时，平台 PV 或 VM virtio 标签任一为真取 defaultPVNicDriver，否则 defaultNicDriver。 |
| VM virtio 开关修改 | 标签增删 | 保留原有显式全局操作语义：增加重设为 PV 型号，删除重设为普通型号，不再用平台 PV 抵消用户的关闭操作。直通 NIC 不参与虚拟型号重设。 |
| 单网卡型号修改 | UpdateVmNicDriver | 仅更新指定 NIC，不修改 VM virtio 标签、其他 NIC 或磁盘；不是运行中热改设备。 |
| 平台修改 | UpdateVmInstance | 使用目标平台及同请求的新 guestOsType 计算默认值；平台字段与所有受影响 NIC 在同一事务完成。单独修改 guestOsType 保持原有“不立即重算”行为。 |
| 后端选择 | VmNicManager.getVmNicType / Factory | 保留 L2/vSwitch、SR-IOV/VFHA、vhostuser、ZNS mode 的现有选择流程；能力登记和校验不替代后端选择。 |

## 后端能力与冲突策略

| 后端 | 能力 | 行为 |
| --- | --- | --- |
| KVM VNIC（含 ZNS kernel） | Emulated | 当前白名单 virtio/e1000/rtl8139/pcnet；架构及 QEMU/libvirt 只有通用宿主/集群约束，尚无逐型号能力矩阵。 |
| TFVNIC | Emulated | Agent ethernet 接口使用显式 model；并非硬件直通。 |
| MACVLAN | Emulated | Agent direct/VEPA 接口使用显式 model；补齐 NicTO 型号与 vHostAddOn。Agent 的 direct 分支还须补齐 device_attr 初始化。 |
| dpdkvhostuserclient（含 ZNS DPDK） | Virtio | 只允许 virtio。 |
| vDPA | Virtio | 当前 Agent 使用 vhost-user，故同样只允许 virtio。 |
| VF/SR-IOV | Passthrough | model 不适用；创建不传虚拟型号，显式指定或单 NIC 修改型号均拒绝。VM 全局开关不改写 VF driverType。 |
| XDragon 覆盖后端 | 额外约束 | vhostSrcPath 生效时要求 virtio；创建/修改在扩展校验，启动在 addons 完整后再次校验。 |
| 未登记类型/能力 | 未知 | 明确拒绝产生新配置；不默认为普通网卡。 |
| ESX | 独立体系 | 仍由 VMwareVmNicManager 处理 E1000/E1000E/Vmxnet3/Sriov；共用分配流程不能套用 KVM 小写型号白名单。 |

自动默认值也必须与后端兼容。例：Windows 或 Other、virtio 关闭、没有插件偏好时默认 e1000；在 vhost-user 上明确失败，需启用镜像/VM virtio 或显式选择 virtio。不能据后端偷偷开启 VM virtio，因为它还影响磁盘；也不能只在启动时把 e1000 改成 virtio，导致 API/DB 与设备不一致。

混合普通/加速 NIC 的全局操作先验证所有候选结果；任一不兼容则整个操作失败，平台/标签/所有 NIC 保持原值。用户可继续按 NIC 修改合法型号，或显式更换网络后端。

## 生命周期入口

| 入口 | 处理 |
| --- | --- |
| 创建 VM、附加 L3/新建 NIC | 先计算并校验，再进入 Factory 创建资源；持久化前重新读取默认值并校验；复用 VM 生命周期队列。Factory 内部持久化使用独立事务，不能在外层持有 VM 行锁。沿用 Factory 原有 OperationFailureException 处理边界；型号校验通过该异常返回业务错误并触发回滚，不在本单扩展其他 Factory 异常的处理语义。 |
| 挂载已有 NIC | 沿用已有非空型号，空值使用默认规则；锁内读取目标 VM 和设备列表；失败回滚绑定、型号和设备字段。 |
| 单 NIC 修改 | 复用 VM 任务队列，锁内核对归属及后端能力；仅持久化预配置。 |
| 全局 virtio 标签 | 复用 SystemTag judger/listener；前置校验、标签变化、NIC 重设在同一事务。状态检查沿用原 API validator：Running/Unknown 时禁止 API 新增；运行中删除仍允许更新下次启动预配置。内部 GuestTools 在停止/重启期间创建标签仍允许，但不能绕过后端能力校验。同值 quickRecreate 保留的标签不触发删除校验或删除事件；GuestTools 显式启用时，在同一事务内复用 NIC 策略入口重设型号，不能依赖已移除的平台后置回调。仅 virtio 标签 opt-in 传播监听错误，避免失败后标签已删而 NIC 未改。禁止重命名到/从该标签绕过增删语义。 |
| 平台修改 | 复用原 UpdateVmInstance 流程，在其数据库事务中校验并更新 NIC，移除旧的后置重复 writer。FT 的 NIC 属于组记录，原 FT 同步流程显式调用相同策略，将组平台与组 NIC 型号放在同一事务，不依赖已移除的 NIC 平台回调；该事务拒绝平台/型号变更时，通过原 Flow 回滚恢复前续已提交的主、副 VM 平台和主 VM guestOsType。 |
| VF 转普通网卡 | 原 API 只允许 VF→VNIC；复用统一默认与校验，校验成功后才释放 VF 绑定并更新。 |
| 启动/停止后启动/管理侧重建式重启 | 保留已保存型号；KVM 扩展和 addons 完整后校验最终 NicTO，不静默纠正 Windows vhost-user。 |
| 运行态热挂载/更新接口 | 在最终命令扩展后校验；卸载不校验型号，允许移除历史错误设备。 |
| 客户机内重启、热迁移 | 沿用运行中的 XML，不重算型号。数据库中的下次启动预配置不代表运行中 XML，不能据此拒绝本来可用的热迁移。 |
| 内存快照设备恢复 | 原归档只包含 VNIC；执行拆卸前验证归档型号；需要重新创建的 NIC 按当前 Factory 选择后端再校验。复用原 VmAttachNicMsg 恢复，不在成功回调中再覆盖 driverType。不能据此宣称支持 VF/vDPA 内存快照。 |
| 升级初始化 | 仅处理旧 VNIC 的 NULL 值；已保存非空型号不自动改写，无法合法推导的记录保留并记录日志，不阻断管理节点启动。 |
| 镜像修改/重装 | 镜像属性不是已存在 NIC 的持续数据源；ReimageVmInstanceMsg 仅向根盘发送 ReInitVolumeMsg，不写 NIC。实际进入新 NIC 分配的流程经过统一校验；其他换盘/克隆变体尚需分别回归。 |

事务由项目现有 AspectJ 模式织入（conf/zstack.xml 与 conf/zstackSimulator.xml），不是仅代理 public 方法的 Spring proxy 模式；故同类 private 方法的事务注解亦生效，仍以失败注入回归验证边界。`DatabaseFacade.persistAndRefresh/updateAndRefresh/remove` 使用 REQUIRES_NEW，不能因外层有事务就认为原子；需要原子性的写入复用 SQLBatch 的 merge/reload/persist 及 removeByPrimaryKeysInCurrentTransaction。设备地址删除也加入同一事务。

## Agent 与历史数据

Agent 对非 hostdev 接口优先使用非空 driverType，空值才按 useVirtio 回退；hostdev 不输出虚拟 model。多队列与队列大小沿用既有逻辑，仅 virtio 型号生成相关 driver 属性，不新增一个型号切换配置体系。

本次控制面负责拒绝新的不兼容配置。旧的非空非法组合不会被批量修正：重建设备时返回包含 NIC UUID/type/model 的错误，用户确认客户机具备 virtio 驱动后，显式改成 virtio；或更换普通网络后端。删除/卸载仍可用于恢复。已经运行的 VM 不因升级被热换设备，现有 XML 与 DB 可能不同，须在计划停机前检查。

宿主机架构与 QEMU/libvirt 的精确支持集、FreeBSD/Windows 驱动安装状态仍是运行前提。本补丁没有引入未经探测的“全型号支持”承诺；现有宿主/网络分配检查继续生效。

## 验证计划与证据边界

- 主仓 VmNicDriverLifecycleCase：复用原 UpdateVmNicDriverCase，增加五平台 × 镜像 virtio 开关 × 请求 virtio 标签 × 显式/默认型号（45 个组合，Other 使用 FreeBSD guestOsType），检查 API、DB、NicTO 和根盘；注入标签监听失败验证回滚。
- premium VhostUserNicDriverLifecycleCase / VdpaNicDriverLifecycleCase：使用现有 OVS/PCI API fixture 创建真实后端，非法型号在创建和修改均失败，检查 VM/NIC/IP/磁盘清理、原值、标签/平台回滚和重启命令。
- MacVlanNicDriverLifecycleCase / VfNicDriverLifecycleCase / ZnsNicDriverLifecycleCase：分别通过原 fixture 创建后端，检查虚拟型号传递、直通拒绝和 ZNS kernel/DPDK 分流；不直接改 VmNicVO.type 冒充设备。
- utility test_nic_driver_xml.py：执行真实 XML 构造函数，检查普通/TF/MACVLAN 的四种型号、vhost-user/vDPA/XDragon 路径与 hostdev 无 model。该测试不连接 libvirt，不是实机验收。

执行结果以交付时的测试记录为准；未完成的用例不得写成通过。FreeBSD/Windows 真客户机启动和网络通信、加速网卡真硬件、架构/QEMU 组合、实际热迁移/内存恢复需独立环境验收。zwatch 和 QGA 网络配置不在本单范围。

## 历史验证记录（2026-10-19）

- 主仓基线 `74dc60d36f`，premium 基线 `2d76ab2e18`，utility 基线 `22218eb64a9c3eb90625882e41f1eeca90bcef3b`。
- 开发机主仓 + premium 144 模块构建通过（`-Ppremium -pl premium/test-premium -am -DskipTests -Dmaven.test.skip=true install`）；此项不包含测试执行。
- utility 真实 XML 构造测试 28/28 通过；修复前 MACVLAN 的 8 个组合因 `device_attr` 未初始化失败。
- 主仓完整回归通过：45 个创建组合，原有修改流程；标签删除监听失败时标签/NIC/设备地址回滚；API 与 inherent 标签创建监听失败回滚；插件默认值优先级、同优先级冲突，以及冲突导致创建失败时无 NIC 泄漏。最终运行 296.519 秒，Failures 0 / Errors 0。
- vhost-user 与 vDPA API/simulator 回归分别通过：各 50 个创建组合，非法修改拒绝，VM/NIC/IP/磁盘清理，混合网卡回滚和历史非法配置修复。
- MACVLAN API/simulator 回归通过：四种现有虚拟型号、NicTO/vHostAddOn 和停止后启动。
- VF API/simulator 回归通过：Windows/Linux 两平台下显式型号创建和单 NIC 修改被拒绝；默认创建的 VF 不带型号；VF 转普通网卡分别使用 e1000/virtio，API 与 DB 一致。
- ZNS API/simulator 回归通过：kernel 保留 virtio/e1000，DPDK 只接受 virtio；非法组合无 NIC 残留，不创建端口且不下发启动命令。
- 单独运行 Case 时应顺序使用独立 Maven/JVM 进程；把多个 Case 直接放入同一个 `-Dtest` 会发生 8989 端口冲突。相同 Spring 配置的子用例可以通过原有 Suite 与 Designated 收集策略运行，使用 `-Dtest=KvmTest`（或 `PremiumTest`）、`-DsubCaseCollectionStrategy=Designated`、`-DcaseFilePath=<完整类名列表文件>`，并逐项核对结果文件。
- 增量构建曾出现普通编译覆盖 woven class 而 AspectJ 跳过的情况；最终使用完整织入产物，并对后续改动模块执行 clean install。

### 回归复现与日志

开发机工作区为 `/home/code/zstac-89798-5.6.0`。先加载 `/root/.bashrc`，主仓在 `test`、premium 在 `premium/test-premium` 下分别运行：

```sh
mvn -B surefire:test -DsurefireArgLine= -Djacoco.skip=true -Dtest=<CaseName>
```

以下路径均为开发机上的日志，保存的是 2026-10-19 当时提交的用例结果；vhost-user 所在原始日志的后续第二个用例有端口冲突，vDPA 的最终结果以独立进程日志为准。

| 用例 | 日志 |
| --- | --- |
| VmNicDriverLifecycleCase | `/tmp/zstac-89798-main-final2.log` |
| VhostUserNicDriverLifecycleCase | `/tmp/zstac-89798-premium-test3.log` 中该用例结果 |
| VdpaNicDriverLifecycleCase | `/tmp/zstac-89798-VdpaNicDriverLifecycleCase.log` |
| MacVlanNicDriverLifecycleCase | `/tmp/zstac-89798-MacVlanNicDriverLifecycleCase.log` |
| VfNicDriverLifecycleCase | `/tmp/zstac-89798-VfNicDriverLifecycleCase-final.log` |
| ZnsNicDriverLifecycleCase | `/tmp/zstac-89798-ZnsNicDriverLifecycleCase-final.log` |

Agent XML 测试在 utility 工作树运行：

```sh
/tmp/zstac-89798-py311/bin/python -m pytest tests/unit/kvmagent/test_nic_driver_xml.py -o addopts='' --tb=short -p no:tests.plugins.ssh_plugin -p no:tests.plugins.agent_plugin
```


## 历史 CI 回归整改（2026-10-20，FT 回滚补充前）

### Root cause

- virtio 标签 judger 新增的 Created/Stopped 限制改变了既有语义：Windows 运行中删除标签、GuestTools 在停止/重启流程中内部创建标签都被额外拒绝。
- 移除主仓 NIC 平台变更后置 writer 后，FT 同步组记录仍依赖该 extension。FT 网卡属于组 UUID，更新主/副 VM 平台并不会同步组网卡。
- NIC Factory 外层从只处理 OperationFailureException 扩为捕获 Exception，改变了不属于型号校验的异常处理语义，导致旧 SR-IOV 用例创建普通 VM 的行为变化。

### Fix solution

- 撤回 judger 的新增状态限制，保留原 API 创建 validator 与后端校验。运行中删除标签仍只修改下次启动配置；停止/重启内部标签创建不再被额外拦截。
- FT 组同步显式复用统一平台策略，在同一 SQLBatch 更新组平台和组网卡，并向其他扩展传递正确的旧平台。该事务只覆盖组记录及组网卡，不代表主/副/组整个更新流程具备全局原子回滚。
- 撤回 Factory 宽泛 catch，恢复原有 OperationFailureException 处理边界。型号校验使用该异常返回业务错误；本单不改变其他 Factory 异常语义或扩大普通 VM 多 VF 支持。

### 基线对照与保留边界

在隔离父提交工作区 `/home/code/zstac-89798-baseline-1020` 实跑，主仓 `74dc60d36f`、premium `2d76ab2e18`。四个旧用例均未修改，且基线均通过：

| 用例 | 基线结果 | 用时 |
| --- | --- | --- |
| ChangeWindowsVmNicDriverCase | 1 test / 0 failures / 0 errors | 418.345 s |
| AttachGuestToolsIsoToVmCase | 1 test / 0 failures / 0 errors | 772.518 s |
| FaultToleranceVmOperationCase | 1 test / 0 failures / 0 errors | 457.376 s |
| TestApplianceVmSriovBondCase | 1 test / 0 failures / 0 errors | 365.448 s |

SR-IOV 基线通过不代表其历史异常处理正确：基线日志已出现 VmVfNicFactory 的多 VF FlowException，经 AsyncSafe / While 后错误未进入 VmAllocateNicFlow 最终判断的 errs 列表，API 返回成功且 vmNics 为空；旧用例断言没有 VmVfNicVO 因此通过。此次仅撤销本单引入的异常边界变化，历史异步错误丢失未整改，不以更改旧用例或增加吞错特判绕过。

新增回归覆盖：VmNicDriverLifecycleCase 的真实 Stopping/Rebooting 内部创建、运行中 API 创建拒绝与删除预配置、DB/启动命令一致；FaultToleranceNicDriverCase 的主 VM/组两条 API 入口与四平台组网卡同步；VmNicFactoryFailureCase 的第二张网卡 Factory 失败及 NIC/IP 回滚。ZSV 按要求不排查、不修改、不运行。

### 最终修复版验证结果

主仓 Suite 3 项、GuestTools 1 项、premium Suite 6 项，共 10 项全部通过。已逐项核对 Designated 用例列表与 success 结果文件，无遗漏或跳过；Surefire 的 Suite 总数为 1，不等于只执行了一个子用例。下表 Suite 子用例用时包含其环境创建/清理，独立进程用时还包含服务启动，不能直接作为性能对比。

| 用例 | 父提交基线 | 最终修复版 |
| --- | --- | --- |
| ChangeWindowsVmNicDriverCase | 通过（418.345 s） | 通过（30.050 s） |
| VmNicDriverLifecycleCase | 本单新增/扩展回归 | 通过（99.941 s） |
| VmNicFactoryFailureCase | 本单新增/扩展回归 | 通过（13.503 s） |
| FaultToleranceVmOperationCase | 通过（457.376 s） | 通过（152.787 s） |
| FaultToleranceNicDriverCase | 本单新增/扩展回归 | 通过（44.978 s） |
| VhostUserNicDriverLifecycleCase | 本单新增/扩展回归 | 通过（174.568 s） |
| VdpaNicDriverLifecycleCase | 本单新增/扩展回归 | 通过（176.189 s） |
| VfNicDriverLifecycleCase | 本单新增/扩展回归 | 通过（48.004 s） |
| TestApplianceVmSriovBondCase | 通过（365.448 s） | 通过（53.457 s） |
| AttachGuestToolsIsoToVmCase | 通过（772.518 s） | 通过（431.448 s） |

开发机修复版工作区 `/home/code/zstac-89798-fixed-1020`，使用独立 Maven 仓 `/home/code/zstac-89798-m2-1020`，避免基线依赖污染。144 模块构建与两仓测试编译通过；最后一处生产修改后对 compute 执行 clean install，最后一处测试修改后重新 test-compile，再运行最终用例。中间测试设置错误的失败结果不计入最终通过记录。

日志目录 `/tmp/zstac-89798-regression-1020/`：基线为 `baseline-<CaseName>.log`；最终为 `final-KvmTest.log`、`final-AttachGuestToolsIsoToVmCase.log`、`final-PremiumTest.log`。Suite 用例列表为该目录的 `main-cases`、`premium-cases`；逐项结果在相应模块 `zstack-integration-test-result/<Suite完整类名下划线形式>/`。

本轮未重跑 MACVLAN、ZNS 与 Agent XML，2026-10-19 的结果只作为历史记录。FreeBSD/Windows 真客户机、加速硬件、数据面通信、架构/QEMU/libvirt 组合、实际迁移/内存恢复均未实机验收；amend 后 MR CI 需另行观察。

## FT 失败回滚与补充回归（2026-10-20）

### Root cause

FT 更新先提交主 VM，再同步副 VM，最后更新 FT 组及组网卡。此前新增的组平台/NIC SQLBatch 能回滚组记录及网卡，但前续主、副 VM 更新已经提交，且副 VM 同步使用 NoRollbackFlow。组网卡校验拒绝目标型号时，API 返回失败，却留下主、副 VM 为 Other、组仍为 Linux、组网卡仍为 virtio 的部分提交。

在本轮修复前的 premium `76c1ce0ec1` 上已通过两种方式复现：一是只在组型号校验扩展点注入失败；二是通过正常 OVS/DPDK API fixture 创建 vhost-user FT，Linux 改为 Other 后被生产校验以 `ORG_ZSTACK_COMPUTE_VM_10346` 拒绝，未注入错误。两种方式均实跑主 VM UUID 与组 UUID 两条 API 入口，并记录修改前后数据库值。这里的“正常后端”指实际执行生产 Factory 和 validator，宿主通信仍由 simulator 承担。

新增快照测试还实测发现一处本单引入的型号丢失：恢复扩展已把归档 e1000 写入 VmAttachNicMsg，但 VmInstanceBase.attachNic 仅从 APIAttachL3NetworkToVmMsg 读取 driverType，未传递内部消息的该字段。删除旧成功回调中的型号覆盖后，分配流程按空值推导 Linux 默认 virtio，导致恢复后的 API/DB/启动命令与归档 e1000 不一致。失败日志明确记录消息中是 e1000、启动命令中是 virtio。

### Fix solution

副 VM 同步改为可回滚的 Flow，保存更新前的副 VM 平台。组平台/NIC 事务因 OperationFailureException 拒绝本次平台变更时，先由原事务回滚组平台及 NIC，再通过 Flow 回滚中的一个 SQLBatch 恢复主、副 VM 平台及主 VM guestOsType。主 VM 旧值来自更新开始前的 VmInstanceSpec；正常成功路径不执行补偿。

此修复覆盖已复现的平台/型号校验失败，不宣称名称、CPU、内存等整个 FT 更新流程具备全局原子性，也不扩展后续其他插件回调抛错的回滚范围。保留取消启动时静默纠正型号的设计。

在现有 attachNic 消息转换处，将 VmAttachNicMsg.driverType 传给 VmNicSpec.nicDriverType，随后仍执行统一默认值和后端能力校验。无需恢复成功回调的数据库覆盖，也不修改 Factory 默认逻辑；未指定型号的内部挂网卡请求仍使用原默认值。新增快照测试在 finally 中删除快照组并销毁、彻底删除动态 VM，避免失败时资源清理错误遮住原始断言。

### 新增测试覆盖

- `FaultToleranceNicDriverRollbackCase`：组校验扩展失败，分别通过主 VM 和组 UUID 更新，检查三条 VM 记录的平台、guestOsType 及全部组 NIC 的 UUID/type/driver 均保持原值。
- `FaultToleranceVhostUserRollbackCase`：真实生产 vhost-user Factory/validator 拒绝不兼容型号，两条 API 入口均检查上述状态不变；不伪造网卡类型。
- `MemorySnapshotNicDriverCase`：Linux 默认 virtio、显式 e1000 网卡归档后删除旧 NIC，恢复时重建新 NIC 并保留 e1000，检查 API/DB/启动命令、IP/MAC/default L3/QoS；另注入 VmAttachNicReply 失败，检查失败回调不改设备地址或 default L3、不发送 QoS 设置消息。
- `VmNicDriverLifecycleCase` 新增停止状态的标签更新检查：普通 hostname 改名成功；改名到/从 `driver::virtio` 均拒绝，原标签和两张 NIC 型号不变。使用停止状态，避免被原有运行态 validator 提前拒绝而漏测改名保护。

旧 `FaultToleranceVmOperationCase`、`MemorySnapshotCase`、`UpdateVmNicDriverCase` 与父提交逐字节一致，没有修改旧用例迁就实现。

### 本轮最终执行结果

compute 与 faulttolerance 完整织入构建、两仓测试编译通过。最终源代码在开发机 API/simulator 上共 7/7 Case 通过，逐项核对 premium Suite 的六个 success 文件与主仓独立用例结果，无失败、跳过或遗漏。两个 FT 回滚 Case 共四组入口检查的修改前后状态完全一致；快照恢复与失败回调专用断言均执行通过。

| 用例 | 结果 | 用时 |
| --- | --- | --- |
| MemorySnapshotNicDriverCase | 通过 | 64.402 s |
| FaultToleranceNicDriverRollbackCase | 通过 | 45.444 s |
| FaultToleranceVhostUserRollbackCase | 通过 | 55.693 s |
| FaultToleranceNicDriverCase | 通过 | 58.620 s |
| FaultToleranceVmOperationCase | 通过 | 101.278 s |
| MemorySnapshotCase | 通过 | 207.722 s |
| VmNicDriverLifecycleCase | 通过 | 284.441 s |

Suite 子用例用时包含环境创建/清理，主仓独立用例用时还包含服务启动，不能直接比较性能。最终执行的两个生产文件与四个新增/扩展测试文件已校验 SHA-256，与提交源码一致。

开发机工作区 `/home/code/zstac-89798-fixed-1020`，隔离 Maven 仓 `/home/code/zstac-89798-m2-1020`。最后的 compute 修复完成 clean install 后，全部六个 premium 用例与主仓用例重新执行。premium 在 `premium/test-premium` 下运行：

```sh
mvn -Dmaven.repo.local=/home/code/zstac-89798-m2-1020 -B surefire:test -DsurefireArgLine= -Djacoco.skip=true -Dtest=PremiumTest -DsubCaseCollectionStrategy=Designated -DcaseFilePath=/tmp/zstac-89798-ft-fix-validation/premium-cases
```

主仓在 `test` 下使用相同 Maven 仓与 Surefire 参数，以 `-Dtest=VmNicDriverLifecycleCase` 独立运行。

最终日志及逐项状态在开发机 `/tmp/zstac-89798-ft-fix-validation/{premium.log,main.log,final-results.json}`。首轮快照 e1000 断言失败及随后清理失败的日志保存在 `attempt1/`；该轮被跳过的旧 MemorySnapshotCase 不计为通过，表中记录的是最终完整重跑结果。FT 修复前两种复现日志分别为 `/tmp/zstac-89798-ft-rollback-repro/test.log`、`/tmp/zstac-89798-ft-vhost-rollback-repro/test.log`。

本轮未重跑 FT StabilityTest（包括 CI 100 轮）、其他普通 Case 或 Agent XML；此前结果仅为历史记录。amend 后 CI 结果尚未取得。FreeBSD/Windows 真客户机、加速硬件/数据面、架构/QEMU/libvirt 组合、实际迁移及内存恢复均未实机验收。ZSV 按要求未处理。
