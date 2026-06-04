---
title: "feat: ZBS vhost 输出协议支持"
type: feat
status: active
created: 2026-06-02
depth: deep
origin: docs/zbs-vhost/10-PRD.md
repos: [zstack (plugin/zbs), zstack-utility (kvmagent)]
---

# feat: ZBS vhost 输出协议支持 — 开发计划

> 源: `docs/zbs-vhost/10-PRD.md` + `docs/zbs-vhost/00-investigation-seed.md`
> 分支: `feature-zbs-vhost` @ 5.5.28（zstack + premium + zstack-utility）
> 路径均 repo-relative（zstack 仓库 / zstack-utility 仓库各自根）

## 问题框定

ZBS 新增 Vhost 输出协议。与 expon「阵列出 socket」不同，ZBS 需 **ZStack 在计算节点
全管 SPDK target 容器 + 发 JSON-RPC 建 bdev/controller 产出 unix socket**，再由通用
`plugin/vhost/KvmVhostNodeServer` + utility `vm_plugin.vhost_volume()`（5.5.28 已有）消费。

净增 = ① utility 的 SPDK target 容器管理 + JSON-RPC（zbs_storage_plugin）；
② zstack 的 `ZbsStorageController` vhost 分支（镜像 expon）。

## 关键技术决策

- **容器编排 ZStack 全管**（kvmagent 插件），不依赖 ZBS 受管服务。
- **重启懒重建**：不做主动状态重建；下次 VM 启动/热插 activate 时自然重建 socket。
- controller 名 / socket = `socketDir/zbs-vhost-<volUuid>`，由 volUuid 推导 → activate 幂等、getActivePath 免查询、懒重建可定位。
- HA 走 CBD 心跳；ISO 走 CBD；QoS/在线扩容走存储侧（capabilities 保持 false）。
- 异步铁律：Java 侧禁 Future/CompletableFuture，统一 `Completion` + `CloudBus.send(msg, callback)`；host 调用走 `KVMHostAsyncHttpCallMsg`。
- 不改原有变量/格式，不污染 git blame；新方法签名向后兼容。

## 高层设计（directional，非实现规范）

```
VM start (Vhost 盘)
  KvmVhostNodeServer.beforeStartVmOnKvm  [plugin/vhost, 不改]
    -> nodeSvc.getActivePath(vol, host)         [zstack ZbsStorageController, U6]
         -> KVMHostAsyncHttpCallMsg /zbs/.../vhost/activate   [U5 cmd]
              -> zbs_storage_plugin.activate    [utility U3]
                   -> ensure target container   [utility U2]
                   -> rpc bdev_zbs_create + vhost_create_blk_controller  [utility U1]
                   -> socket = socketDir/zbs-vhost-<volUuid>
         <- socket path
    -> VolumeTO.installPath = socket; useHugePage+shared  [plugin/vhost, 不改]
  vm_plugin.vhost_volume() -> <disk type=vhostuser ...>   [utility, 5.5.28 已有, U7 复核]
```

## 实现单元

### U1. SPDK JSON-RPC 客户端 helper（utility）

- **Goal**: 封装对 target 控制 sock（`vhost.sock`）的 JSON-RPC 调用。
- **Dependencies**: 无
- **Files**:
  - `kvmagent/kvmagent/plugins/zbs_vhost_rpc.py`（新）
  - `kvmagent/kvmagent/test/test_zbs_vhost_rpc.py`（新）
- **Approach**: 方法 `bdev_zbs_create(zbs_path, name)` / `bdev_zbs_delete(name)` / `bdev_get_bdevs(name=None)` / `vhost_create_blk_controller(ctrl, bdev)` / `vhost_delete_controller(ctrl)` / `bdev_resize(name, size)`。统一 JSON-RPC over unix socket；RPC 名以手工实验 spdk-rpc 工具实际命令为准（运行期核对，bdev_zbs_* 为 ZBS 自定义）。返回结构化结果，错误抛 `kvmagent.KvmError`。
- **Patterns**: 参照现有 plugin 里 socket/子进程封装与 `@lock.lock` 用法（`storage_device.py`）。
- **Test scenarios**:
  - bdev_zbs_create 正常返回设备名；重复创建同 zbs_path 幂等（返回已存在设备，不报错）。
  - vhost_create_blk_controller 正常；ctrl 已存在返回现有不报错。
  - delete 不存在的 bdev/controller → 幂等成功（ENOENT 当成功）。
  - sock 不可达 → KvmError 明确报错。
  - bdev_resize 正常改大小。
- **Verification**: 单测对 mock sock 全绿；幂等/ENOENT 路径覆盖。

### U2. SPDK target 容器生命周期（utility）

- **Goal**: 在计算节点确保 `zbs-vhost` 容器运行（load + run + ensure + stop）。
- **Dependencies**: 无
- **Files**:
  - `kvmagent/kvmagent/plugins/zbs_vhost_target.py`（新）
  - `kvmagent/kvmagent/test/test_zbs_vhost_target.py`（新）
- **Approach**: `ensure_target(cores, socket_dir, client_conf, image)`：镜像不在则 `docker load`；容器未跑则 `docker run -d --privileged --network host -v hugepages -v socket_dir -v client.conf vhost -m <cores> -S <socket_dir> -r <ctrl_sock> -z client.conf`（参手工实验第 2 步）。`is_running()` / `stop_target()`。hugepage 校验复用 `host_plugin.enable_hugepage`。
- **Patterns**: 现有 docker/bash 调用封装；`host_plugin` hugepage。
- **Test scenarios**:
  - 镜像缺失 → 触发 load；已存在 → 跳过。
  - 容器未跑 → run；已跑 → 幂等不重复 run。
  - hugepage 不足 → 明确报错。
  - 绑核参数正确拼接。
  - stop 后 is_running 为 false。
- **Verification**: 单测覆盖 ensure 幂等 + 各前置缺失分支。

### U3. zbs_storage_plugin vhost 端点（utility）

- **Goal**: 暴露 activate/deactivate/resize/target-ensure HTTP 端点，编排 U1+U2。
- **Dependencies**: U1, U2
- **Files**:
  - `kvmagent/kvmagent/plugins/zbs_storage_plugin.py`（改：注册新端点 + handler）
  - `kvmagent/kvmagent/test/test_zbs_storage_plugin_vhost.py`（新）
- **Approach**: 新 path `/zbs/primarystorage/vhost/target/ensure`、`/vhost/activate`、`/vhost/deactivate`、`/vhost/resize`。activate = ensure_target → bdev_zbs_create → vhost_create_blk_controller → 返回 `socket = <socket_dir>/zbs-vhost-<volUuid>`（ctrl 名同此）。deactivate = vhost_delete_controller → bdev_zbs_delete。resize = bdev_resize。沿用现有 `CHECK_HOST_STORAGE_CONNECTION_PATH` 注册风格。
- **Patterns**: 同文件现有 `register_async_uri` + AgentResponse 风格。
- **Test scenarios**:
  - activate 返回推导 socket；重复 activate 同卷返回同 socket、不泄漏 bdev/controller。
  - deactivate 后 bdev_get_bdevs 无残留；对未激活卷 deactivate 幂等成功。
  - resize 改 bdev 大小返回成功。
  - target 未跑时 activate 先 ensure。
  - rpc 失败 → AgentResponse.success=false 带错误。
- **Verification**: 单测全绿；activate→deactivate 往返无残留。

### U4. ZbsConstants + GlobalConfig（zstack）

- **Goal**: vhost 常量与可配置项。
- **Dependencies**: 无
- **Files**:
  - `plugin/zbs/src/main/java/org/zstack/storage/zbs/ZbsConstants.java`（改）
  - `plugin/zbs/src/main/java/org/zstack/storage/zbs/ZbsGlobalProperty.java`（改）
  - GlobalConfig（若有 `ZbsGlobalConfig`，无则按现有配置约定新增）
- **Approach**: socket 目录、ctrl 名前缀 `zbs-vhost-`、镜像名/路径、spdk-rpc 端点 path 常量、默认绑核数、vhost 启用开关。
- **Test scenarios**: `Test expectation: none -- 纯常量/配置，无行为`（GlobalConfig 校验逻辑若有则补单测）。
- **Verification**: 编译通过；配置项可读默认值。

### U5. vhost host 调用 Cmd/Rsp（zstack）

- **Goal**: 与 U3 端点对应的命令对象。
- **Dependencies**: U4
- **Files**:
  - `plugin/zbs/src/main/java/org/zstack/storage/zbs/` 下新增（参现有 `DeployClientCmd`/`CbdToNbdCmd` 同处定义风格）：EnsureVhostTargetCmd/Rsp、VhostActivateCmd/Rsp、VhostDeactivateCmd/Rsp、VhostResizeCmd/Rsp。
- **Approach**: 字段：volUuid、zbsPath、size、cores、socketDir、clientConf 等；Rsp 带 socketPath。命名/位置对齐现有 ZBS cmd。
- **Test scenarios**: `Test expectation: none -- 纯 DTO`。
- **Verification**: 编译通过；字段与 U3 JSON 对齐。

### U6. ZbsStorageController vhost 分支（zstack）

- **Goal**: 控制器支持 Vhost 协议（镜像 expon）。
- **Dependencies**: U5；运行期依赖 U3
- **Files**:
  - `plugin/zbs/src/main/java/org/zstack/storage/zbs/ZbsStorageController.java`（改）
- **Approach**（参照 expon `plugin/expon/.../ExponStorageController.java` L177 activate / L194 activeVhostVolume / L444 getActivePath / L480 getActiveVolumeInfo / L502 getActiveVolumesLocation）：
  - `activate`: 加 `VolumeProtocol.Vhost` 分支 → 发 `VhostActivateCmd`（含 ensure target）经 `KVMHostAsyncHttpCallMsg` 到 host → 回 `VhostVolumeTO(socket)`。
  - `deactivate`: Vhost 分支 → 发 `VhostDeactivateCmd`。
  - `getActivePath`: Vhost → 返回推导 socket `socketDir/zbs-vhost-<volUuid>`（免查询）。
  - `getActiveVolumeInfo` / `getActiveVolumesLocation`: Vhost 分支（从 socket 反解 volUuid / 返回 socket glob）。
  - `expandVolume`: 活动为 Vhost 时存储侧扩容后追加 `VhostResizeCmd`。
  - `reportCapabilities`: 输出协议集合加 Vhost（QoS/resize 保持 false）。
  - 异步铁律：全程 Completion + CloudBus callback，禁 Future。
- **Patterns**: expon vhost 方法；本文件现有 CBD 分支与 `httpCall`/`KVMHostAsyncHttpCallMsg` 用法（deployClient L210、cbdToNbd L1136）。
- **Test scenarios**（Groovy SubCase，见 U9，此处列行为）:
  - activate(Vhost) 经 simulator host 回 socket，VolumeTO 路径正确。
  - deactivate(Vhost) 触发 delete controller+bdev。
  - getActivePath(Vhost) 返回推导 socket（不发 RPC）。
  - expandVolume(Vhost) 触发存储侧 + bdev resize。
  - reportCapabilities 含 Vhost。
  - activate(CBD) 回归不受影响。
- **Verification**: 编译过；Groovy 用例（U9）绿；CBD/NBD 回归绿。

### U7. ZbsStorageFactory discover + 输出协议注册（zstack）+ utility 消费端复核

- **Goal**: ZBS 主存储可发现/添加 Vhost 输出协议；确认 vm_plugin 适配 ZBS socket。
- **Dependencies**: U6
- **Files**:
  - `plugin/zbs/src/main/java/org/zstack/storage/zbs/ZbsStorageFactory.java`（改：discover 输出协议加 Vhost）
  - `kvmagent/kvmagent/plugins/vm_plugin.py`（复核 `vhost_volume()`，预计不改）
- **Approach**: discover 返回 outputProtocols 含 Vhost；确保 `PrimaryStorageOutputProtocolRefVO` 可注册 Vhost（沿用 `APIAddStorageProtocol` 现有通道）。复核 vm_plugin `vhost_volume()` 对 ZBS socket 路径（`os.path.exists` + `<source type=unix>`）无需特化。
- **Test scenarios**:
  - discover 输出协议含 Vhost。
  - 添加 Vhost 输出协议后可建 Vhost 盘。
  - vm_plugin vhost_volume 对给定 socket 生成正确 `<disk type=vhostuser>`（若已有单测则补一例 ZBS 路径）。
- **Verification**: discover 用例绿；vm_plugin 复核结论记录（改/不改）。

### U8. 镜像分发（utility 打包/部署）

- **Goal**: `zbs-vhost` + `spdk-rpc` 镜像随包到节点，可被 U2 load。
- **Dependencies**: U2
- **Files**:
  - `zbsprimarystorage/ansible/zbsp.py` 或 kvmagent bootstrap（按现有 ZBS deployer 约定）
- **Approach**: 镜像 tar 纳入产物/仓库；`deployClient`/host dependency 流程确保镜像就位。x86/ARM 双架构（依赖 ZBS 提供，见 PRD §6.3）。
- **Test scenarios**: `Test expectation: none -- 打包/部署，集成环境验证`（记录手动验证步骤）。
- **Verification**: 节点上镜像可 `docker load` 成功；U2 ensure 能拉起。

### U9. Groovy SubCase 集成测试（zstack）

- **Goal**: 覆盖 Vhost 分支真实行为（非 API）。
- **Dependencies**: U6, U7
- **Files**:
  - `plugin/zbs/src/test/.../ZbsVhost*Case.groovy`（新，按现有 ZBS Groovy 用例目录）
  - simulator 扩展：host vhost 端点模拟回 socket
- **Approach**: env 起 ZBS PS + Vhost 输出协议 + KVM host simulator；用例：创建/激活/去激活/扩容/容量上报；断言 socket 路径推导、bdev/controller 编排调用、capabilities 含 Vhost、HA 仍 CBD 心跳。
- **Execution note**: 测真行为（socket 推导、扩容、去激活清理），勿测 API 形状。
- **Test scenarios**: 见 U6/U7 行为列表，逐条落 SubCase；多用例串行（IT 串行铁律）。
- **Verification**: `run-case.sh` 串行全绿。

## 依赖与顺序

```
U1 ┐
U2 ┘─> U3                 (utility 链)
U4 ─> U5 ─> U6 ─> U7      (zstack 链, U6 运行期依赖 U3)
U2 ─> U8
U6,U7 ─> U9
```
utility 链（U1→U3）与 zstack 链（U4→U6）可并行开发；联调需 U3+U6 同时就位。

## 系统级影响

- KVM host: 多一个常驻 SPDK 容器（CPU 绑核 + ~320M hugepage），需容量规划。
- 升级/运维：宿主机重启后运行中 Vhost VM 需重启/迁移恢复（懒重建限制，文档必写）。
- 回归面: CBD/NBD 既有路径、HA CBD 心跳、ISO CBD。

## 范围边界

### 本期不做
- target 重启主动状态重建（懒重建）
- 多池容量精细分配
- 虚拟化侧 QoS / 在线扩容
- ISO over vhost

### Deferred to Follow-Up Work
- 重启主动重建（待 ZBS 提供 save/load config 或 list RPC 后做）
- 健康/心跳 RPC 接入 PS-host 多协议状态
- 共享绑核多卷模式优化

## 风险

| 风险 | 等级 | 缓解 |
|---|---|---|
| 懒重建：宿主机重启 Vhost VM I/O 挂起 | P1 | 文档明确；建议重启前迁移/关停；后续接 ZBS 重建 RPC |
| ZStack 全管容器（崩溃/绑核/hugepage） | P1 | U2 幂等 ensure + 容量校验 + 明确报错 |
| RPC 名/语义与 spdk-rpc 工具实际不符 | P2 | U1 运行期核对手工实验命令；bdev_zbs_* 向 ZBS 确认 |
| ZBS 镜像 x86/ARM 交付节奏 | P2 | PRD §6 列为对 ZBS 需求，提前对齐 |

## 已验证（live ZBS 环境 172.24.251.23，2026-06-02）

> 镜像 `zbs-vhost:03e24d67b` + `spdk-rpc:03e24d67b`（dev registry 172.26.208.212:5000）。U1/U2/U3 已在真实 ZBS 集群跑通。

- **SPDK RPC 名/参数**（spdk-rpc 工具 rpc.py，纯 python 裸 socket 直连即可，无需 docker）：
  - `bdev_zbs_create {file, name}` — file = `<逻辑池>/<卷名>_<user>_`（user 默认 zbs，即 `_zbs_` 后缀，与 `check_host_storage_connection` 的 cbd 路径约定一致）；返回 bdev 名
  - `bdev_zbs_delete {name}` / `bdev_zbs_resize {name, new_size}`（new_size 单位 **MiB**，**只扩不缩**，缩报 -22）
  - `vhost_create_blk_controller {ctrlr, dev_name}` / `vhost_delete_controller {ctrlr}` / `vhost_get_controllers {name?}` / `bdev_get_bdevs {name?}`
- **socket = `<socketDir>/<ctrlr名>`**（已确认 controller 创建后即在 socketDir 生成同名 unix socket）。
- **幂等性 SPDK 层不保证**：重复 create controller / delete 缺失项均返回 -32602；缺失 bdev 查询返回 -19；缺失 controller 查询返回 -32603 → 必须 plugin 层 get-then-act（U3 已实现）。
- **target 启动**：`docker run -d --privileged --network host -v socketDir -v /dev/hugepages -v client.conf <img> /usr/local/bin/vhost -m '[cores]' -S socketDir -r ctrlSock -z client.conf`。
- **hugepage**：内存紧张宿主机直接写 nr_hugepages 会得 0，需先 `drop_caches` + `compact_memory` 再写（U2 已实现）；target ~需 160×2MiB(320M)，默认配 256。
- **孤儿 socket**：target 重启前 socketDir 残留旧 control/controller socket 会骗过 readiness → U2 起容器前清空 socketDir + readiness 改真连测试。

## 待确认（实现期）
- ~~spdk-rpc 命令名/参数~~ → 已验证（见上）。
- 是否需 `sdk-regen`：本期仅加输出协议值 + capabilities，预计无新 `API*Msg`；U7 落地后用 `/sdk-regen` 核验。
- `PrimaryStorageOutputProtocolRefVO` 是否需 DB 兼容处理（新协议值）→ U7 核。
- 端到端 vhost-user-blk 挂 VM 需 compute 节点（当前两套环境是 ZBS+MN 一体机，无 kvmagent/qemu）；U1/U2/U3 已在 ZBS 侧验证，VM 挂载验证待 compute 环境。
