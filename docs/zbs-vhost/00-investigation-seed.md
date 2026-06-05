# ZBS vhost 支持 — 调研结论 (seed for PRD/plan)

> base: 三仓库 `feature-zbs-vhost` @ upstream/5.5.28
> 手工实验: confluence pageId=199365840 (zbs vhost使用说明)
> 架构参考: confluence pageId=150518249 (存储对接架构 / external PS addon)

## 1. 目标

ZBS 存储新增 **vhost** 输出协议，让 ZBS 卷以 vhost-user-blk 接入 qemu 虚拟机
（当前 ZBS 只有 CBD = qemu 进程内 libcbd 驱动，和 NBD = 镜像导出）。

## 2. 架构定位 (external PS addon framework)

- ZBS 已通过 external PS 框架接入：`plugin/zbs/ZbsStorageController` 同时实现
  `PrimaryStorageControllerSvc`(控制面) + `PrimaryStorageNodeSvc`(节点面)。
- vhost 协议模块 `plugin/vhost/KvmVhostNodeServer` 是**通用**的：VM 启动时对
  `protocol==Vhost` 的盘调 `nodeSvc.getActivePath()` 拿 socket 路径塞进 VolumeTO，
  并自动 `useHugePage=true` + `memAccess=shared`。**不用改**。
- expon(`plugin/expon/ExponStorageController`) 已有完整 vhost 实现 = **镜像样板**
  (L168 getOrCreateVhostController / L194 activeVhostVolume / L213 VhostVolumeTO /
   vhostSocketDir=`/var/run/wds/` / L480 getActivePath 按 socket 前缀判断)。

## 3. ZBS vhost vs expon vhost：核心差异

| 维度 | expon | **ZBS** |
|---|---|---|
| vhost target 在哪 | 存储阵列 USS gateway (厂商进程) | **计算节点上的 SPDK docker 容器 (zbs-vhost)** |
| socket 谁造 | 阵列 REST 绑定后自动出现 | **本机 SPDK `vhost_create_blk_controller` 生成** |
| ZStack 管容器吗 | 不用 | **要**: 镜像 load + docker run(hugepage/host-net/绑核/client.conf) + 生命周期 |
| 控制面 | 管理节点调阵列 REST | **宿主机发 SPDK JSON-RPC** (bdev + controller) |

→ expon「阵列重、节点轻」；**ZBS「节点重」——多一整套本地 SPDK target 容器 + JSON-RPC，全是净新增**。

## 4. socket 来源 (手工实验第 4→5 步)

`vhost_create_blk_controller <ctrl名> <bdev名>` 执行后，在 `-S` 指定目录下生成
**和 controller 名同名的 unix socket** (如 `/var/tmp/vhost-sockets/vhost-blk-<volUuid>`)。
qemu 用 `<disk type='vhostuser'><source type='unix' path=该socket>` 接入。
手工实验**无任何**「存储下发 socket」步骤 —— socket 100% 来自本机第 4 步。

## 5. 手工 9 步 → 自动化归属

| 步骤 | 内容 | 归属 / 现状 |
|---|---|---|
| 1 load 镜像 | zbs-vhost.tar.gz + spdk-rpc.tar.gz | utility 新增 (随包分发) |
| 2 起 target 容器 | docker run --privileged --net host -v hugepages/socket/client.conf -m 绑核 | utility 新增 |
| 3 加 bdev | spdk rpc 提供 zbs 卷路径 → bdev | utility 新增 (JSON-RPC) |
| **4 加 vhost controller** | `vhost_create_blk_controller` → **产出 socket** | utility 新增 (JSON-RPC) |
| 5 接入 qemu | vhost-user-blk + hugepage | ✅ 已有 (5.5.28 `vhost_volume()` + `validate_vhost_attach_requirements()`) |
| 6 扩容 bdev | bdev resize (vhost 不支持虚拟化侧扩容) | utility 新增 + Java 路由 |
| 7 拔出 qemu | virsh detach | ✅ 已有 |
| 8 删 controller | `vhost_delete_controller` → socket 消失 | utility 新增 |
| 9 删 bdev | 删块设备 rpc | utility 新增 |

## 6. 5.5.28 实测缺口

- zstack `ZbsStorageController`: vhost 分支 = **0** (净新增)
- utility `zbs_storage_plugin.py`: spdk/vhost/bdev = **0**，仅 2 端点
  (`/zbs/primarystorage/check/host/connection`, `/zbs/primarystorage/host/updatedependency`)
- utility `vm_plugin.py`: `vhost_volume()` ✅ + 原生 `cbd_volume()` ✅ 已在 5.5.28
- expon vhost: ✅ 完整可参照

## 7. 改动清单

### zstack (plugin/zbs)
- `ZbsStorageController`: `activate/deactivate/getActivePath` 加 Vhost 分支 (镜像 expon)；
  `reportCapabilities` 输出协议加 Vhost；`expandVolume` vhost 走 bdev resize；
  ISO/HA 复用 CBD (vhost 不支持 readonly / VM 外访问)
- `ZbsConstants`: socket 目录 / 镜像名 / spdk-rpc 路径 / 默认绑核
- 新增 Cmd/Rsp: EnsureTarget / Activate(bdev+controller) / Deactivate / Resize
- `ZbsStorageFactory`: discover 输出协议加 Vhost；fencer 仍 CBD
- GlobalConfig: vhost 开关 / 核数 / hugepage / 镜像路径
- `KvmVhostNodeServer`: 预计不改

### zstack-utility (kvmagent)
- `zbs_storage_plugin.py` 扩展 (SPDK target 落点):
  - 容器生命周期 (docker load/run/stop, hugepage, 绑核, socket 目录, client.conf)
  - SPDK JSON-RPC 客户端 (bdev_zbs_create / bdev_get_bdevs / vhost_create_blk_controller /
    vhost_delete_controller / bdev_zbs_delete / bdev resize)
  - 新端点: vhost/target/ensure, vhost/activate, vhost/deactivate, vhost/resize
- `vm_plugin.py` vhost_volume: 复核 ZBS socket 是否适配 (预计已通用)
- 镜像分发: bootstrap/ansible 或 zbsps deployer

## 8. 给 ZBS 团队的需求 (降低对接成本，按价值)

1. target 做成受管服务/systemd，而非裸镜像
2. ⭐崩溃/重启后 bdev+controller 状态重建 (save_config/load_config 或 list RPC) —— 最大运维风险
3. RPC 幂等 + socket 名可由 volume uuid 推导 (省查询、可重试)
4. 共享绑核多卷模式 + 核数公式 (避免一盘一核)
5. 健康/心跳 RPC (供 PS-host 状态 + HA)；vhost 路径能否当心跳卷
6. client.conf 热加载 + 单 target 多池/多集群
7. 离线镜像包 + 稳定 tag，x86/ARM 对等
8. 在线 bdev resize 是否触发 guest 容量变更事件
9. 优雅 drain/detach + 孤儿 socket 清理
10. 活动卷快照一致性 (是否需 quiesce)

## 9. 风险 / 待决

- 容器编排谁扛 (ZStack vs ZBS 受管服务) → 影响工作量级
- target 重启状态重建 (第 8.2 项) 必须前置和 ZBS 谈定
- HA: vhost 卷 VM 外不可访问，心跳沿用 CBD (同 expon 用 iscsi 兜底)
- 容量/QoS: vhost 不支持虚拟化侧，走存储侧 (capabilities 已 false)
