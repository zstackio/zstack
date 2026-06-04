# PRD — ZBS 存储 vhost 输出协议支持

- 日期: 2026-06-02
- 分支: `feature-zbs-vhost` @ 5.5.28（zstack + premium + zstack-utility）
- 关联: confluence 199365840（手工实验）、150518249（external PS addon 架构）
- 调研依据: `docs/zbs-vhost/00-investigation-seed.md`

## 1. 目标

ZBS 存储新增 **Vhost** 输出协议，使 ZBS 卷以 vhost-user-blk 协议接入 qemu 虚拟机，
绕过 qemu 进程内 libcbd（CBD 协议）数据路径，获得 SPDK 用户态高 IOPS 通路
（研发实测单卷 fio 4k randread 峰值 ~650K IOPS）。

非目标：替换 CBD。CBD 仍是默认协议，Vhost 为可选高性能输出协议。

## 2. 背景

- ZBS 已通过 external PS addon 框架接入，当前输出协议：CBD（VM 盘+ISO，qemu 原生 cbd 驱动）、NBD（镜像导出）。
- vhost 协议模块 `plugin/vhost`（`KvmVhostNodeServer`）通用：VM 启动对 `protocol==Vhost` 盘调 `nodeSvc.getActivePath()` 取 socket，自动 hugepage+shared。无需改。
- utility 5.5.28 已有 vhost 消费端：`vm_plugin.py` 的 `vhost_volume()`（`<disk type='vhostuser'>`）+ `validate_vhost_attach_requirements()`。
- expon（`plugin/expon/ExponStorageController`）已有完整 vhost 实现 = 本期镜像样板。

## 3. ZBS vhost vs expon vhost（核心差异）

| 维度 | expon | ZBS（本期） |
|---|---|---|
| vhost target 位置 | 存储阵列 USS gateway（厂商进程） | **计算节点上的 SPDK docker 容器（zbs-vhost）** |
| socket 谁造 | 阵列 REST 绑定后自动出现 | **本机 `vhost_create_blk_controller` 生成同名 unix socket** |
| 容器编排 | 不归 ZStack | **ZStack 全管**（load+run+绑核+hugepage+生命周期） |
| 控制面 | 管理节点调阵列 REST | **宿主机发 SPDK JSON-RPC**（bdev + controller） |

结论：expon「阵列重、节点轻」；ZBS「节点重」——多一整套本地 SPDK target 容器 + JSON-RPC，是本期主要净新增。

## 4. 范围

### 4.1 In scope
- ZBS 新增 Vhost 输出协议；VM root/data 盘可用 vhost-user-blk。
- utility `kvmagent/kvmagent/plugins/zbs_storage_plugin.py` 扩展：
  - SPDK target 容器全生命周期：镜像 load、`docker run`（`--privileged --network host`、hugepage、绑核、socket 目录、`/etc/zbs/client.conf` 挂载）、启停。
  - SPDK JSON-RPC 客户端：bdev 增删（提供 zbs 卷路径）、`vhost_create_blk_controller`/`vhost_delete_controller`、bdev resize、bdev_get_bdevs。
  - 新端点（命名沿用 `/zbs/primarystorage/vhost/*`）：target/ensure、activate、deactivate、resize。
- zstack `plugin/zbs/ZbsStorageController`：`activate`/`deactivate`/`getActivePath`/`expandVolume` 的 Vhost 分支（镜像 expon）；`reportCapabilities` 输出协议加 Vhost；`ZbsStorageFactory` discover 加 Vhost。
- controller 名 / socket 路径由 volUuid 推导（幂等、可懒重建、`getActivePath` 免查询）。
- 在线扩容走 bdev resize；HA 沿用 CBD 心跳；ISO 沿用 CBD。
- 镜像分发（随包/ansible/deployer）。

### 4.2 Out of scope（本期）
- **target/宿主机重启的主动状态重建** → 采用**懒重建**：下次 VM 启动/热插时 activate 自然重建 socket。运行中 VM 在 target 重启期间持续 I/O 不可用（已知限制，见 §8）。
- 多池容量精细分配（沿用现有单池逻辑）。
- 虚拟化侧 QoS / 在线扩容（vhost 不支持，走存储侧；capabilities 保持 false）。
- ISO over vhost（vhost 不支持 readonly disk）。

## 5. 用户场景
1. 管理员给 ZBS 主存储添加 Vhost 输出协议；按盘/按 DiskOffering 选 Vhost。
2. 创建/启动 VM，Vhost 盘 → kvmagent 确保 target 容器在跑 → activate（bdev+controller）→ 返回 socket → VM 以 vhost-user-blk 挂载（hugepage+shared 自动配）。
3. 在线扩容 Vhost 盘 → 存储侧扩容 + bdev resize。
4. 热插/热拔 Vhost 数据盘（VM 已开 hugepage+shared，否则提示关机重试）。
5. 删除/迁移盘 → deactivate（删 controller+bdev，socket 消失）。

## 6. 对 ZBS 团队的功能需求
> 写入 PRD 备案；非本期 ZStack 阻塞项，但强烈建议同步推进。
1. RPC 幂等 + controller/socket 命名可由 volUuid 推导（支持重试与懒重建）。
2. 健康/心跳 RPC：target 存活 + 每 bdev I/O 健康（供 PS-host 状态、HA）。
3. 离线镜像包（zbs-vhost + spdk-rpc）+ 稳定版本 tag，x86/ARM 对等。
4. 在线 bdev resize 是否触发 guest virtio-blk 容量变更事件（免重启），需文档化。
5. 活动卷快照一致性（vhost 服务中打存储侧快照是否需 quiesce）。
6. client.conf 热加载 + 单 target 多池/多集群。
7. 优雅 drain/detach + 孤儿 socket 清理。
8. （未来优化，非硬需求）save/load config 状态重建——本期选懒重建，故降级。

## 7. 验收标准
- ZBS 主存储可添加 Vhost 输出协议；Vhost 盘 VM 可正常创建/启动/挂载/IO。
- activate 幂等：重复 activate 同卷返回同一 socket，不报错、不泄漏 bdev/controller。
- deactivate 后 controller+bdev+socket 清理干净（`bdev_get_bdevs` 无残留）。
- 在线扩容：存储侧 + bdev resize 后容量正确。
- 热插/热拔 Vhost 数据盘成功；VM 未开 hugepage+shared 时给出明确报错。
- target 容器异常退出 → kvmagent `target/ensure` 能重新拉起；新 VM 启动可懒重建。
- HA：Vhost 卷场景下 CBD 心跳仍正常触发 fencing。
- 回归：CBD/NBD 既有路径不受影响。
- Groovy SubCase 覆盖 activate/deactivate/扩容/容量上报的 Vhost 分支。

## 8. 风险与限制
- **懒重建限制**（P1）：宿主机/target 重启后，运行中 VM 的 Vhost 盘需重启 VM 或手动 activate 才恢复；期间 I/O 挂起（VM 侧 reconnect 等待）。必须在升级/运维文档明确，并建议宿主机重启前迁移/关停 Vhost VM。
- **ZStack 全管容器**（P1）：崩溃恢复、CPU 绑核冲突、hugepage 容量（target ~320M + VM 大页）全压 kvmagent，需健壮处理与容量校验。
- **资源占用**：target 满载占近 1 整核/轮询线程；需绑核策略与核数配置，避免一盘一核。
- **ZBS 交付依赖**：离线镜像包与版本 tag 需 ZBS 提供，否则部署受阻。

## 9. 已决策记录
- 容器编排：**ZStack 全管**（kvmagent 插件），不依赖 ZBS 交付受管服务。
- 重启恢复：**懒重建**，本期不做主动状态重建。
- HA / ISO：沿用 **CBD**（vhost 卷 VM 外不可访问 / 不支持 readonly）。
- QoS / 在线扩容：走**存储侧**（capabilities false）。

## 10. 改动文件清单（指引，细节交 ce-plan）
### zstack `plugin/zbs/`
- `ZbsStorageController.java` — activate/deactivate/getActivePath/expandVolume Vhost 分支 + reportCapabilities
- `ZbsConstants.java` — socket 目录 / 镜像名 / spdk-rpc 路径 / 默认绑核
- 新增 Cmd/Rsp — EnsureTarget / Activate / Deactivate / Resize
- `ZbsStorageFactory.java` — discover 输出协议加 Vhost
- `ZbsGlobalProperty` / GlobalConfig — vhost 开关 / 核数 / hugepage / 镜像路径
- `KvmVhostNodeServer.java`（plugin/vhost）— 预计不改

### zstack-utility `kvmagent/`
- `kvmagent/plugins/zbs_storage_plugin.py` — SPDK target 容器管理 + JSON-RPC + 4 端点
- `kvmagent/plugins/vm_plugin.py` — `vhost_volume()` 复核 ZBS socket 适配（预计已通用）
- 镜像分发 bootstrap/ansible 或 zbsps deployer
