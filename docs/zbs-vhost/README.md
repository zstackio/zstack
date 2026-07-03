# ZBS Vhost 输出协议

ZBS 存储新增 **Vhost** 输出协议：ZBS 卷以 vhost-user-blk 接入 qemu，绕过 qemu 进程内
libcbd（CBD 协议）数据路径，走 SPDK 用户态高 IOPS 通路。CBD 仍为默认协议，Vhost 为可选高性能协议。

## 1. 架构

- ZBS 经 external PS addon 框架接入：`plugin/zbs/ZbsStorageController` 同时实现
  `PrimaryStorageControllerSvc`（控制面）+ `PrimaryStorageNodeSvc`（节点面）。
- 通用 vhost 模块 `plugin/vhost/KvmVhostNodeServer`（5.5.28 已有，不改）：VM 启动时对
  `protocol==Vhost` 的盘调 `nodeSvc.getActivePath()` 取 unix socket，塞进 VolumeTO 并自动
  `useHugePage=true` + `memAccess=shared`。utility 消费端 `vm_plugin.vhost_volume()`
  （`<disk type='vhostuser'>`，5.5.28 已有）落地 domain XML。
- **数据面 SPDK target 由 zbsadm（ZBS PS 管理节点）编排**：zbsadm 从管理节点 SSH 到计算主机，
  拉起 SPDK vhost docker 容器（`zbsvhost-<host ip>`）、发 JSON-RPC 建 `bdev_zbs` + vhost
  controller，产出 `<VHOST_SOCKET_DIR>/<bdev name>` 的 unix socket（socket 以 bdev 命名，无独立
  controller 名）。控制面走 zbsadm 路径（`CREATE_VHOST_BDEV_PATH` / `DELETE_VHOST_BDEV_PATH`
  / `DEPLOY_VHOST_PATH`），非计算侧 KVM-host 路径。
- kvmagent 仅负责主机环境预备（`prepare_vhost_target_env`：hugepages + docker）和一个本机
  健康探针（`vhost_target_health`：容器在跑 + admin socket 应答），zbsadm 只检查不安装环境。

### 关键决策
- 容器编排归 zbsadm（ZBS 侧），ZStack 触发/探活；cpuset 由目标主机 CPU 数推导，hugepage 走 zbsadm 默认。
- 重启恢复走**懒重建**：不做主动状态重建，下次 VM 启动/热插 activate 时自然重建 socket。
- HA 走 CBD 心跳；ISO 走 CBD（vhost 不支持 readonly disk）；QoS/在线扩容走存储侧（capabilities 保持 false）。
- 整机（host-level）连通状态取 **any-protocol-connected**（任一协议健康即整机 Connected），
  使无 vhost target 的主机仍能通过 CBD 访问存储；按协议的独立明细见 §3。

## 2. 数据模型

- `ExternalPrimaryStorageHostProtocolRefVO`：per-(primaryStorage, host, protocol) 一行，
  三元组唯一，FK 级联；每协议独立记 `status`（Connected / Connecting / Disconnected）。
- `VolumeVO.protocol`：每卷记录自己的协议。
- `ExternalPrimaryStorageHostRefVO.protocol` 死列已 DROP（历史遗留，无代码读写）。
- 整机 ref 保持折叠语义（历史行为不变），按协议明细一律以 protocol 表为准。

## 3. 协议枚举

```
NVMEoF | iSCSI | Vhost | CBD | NBD | RBD
```
严格区分大小写。一套 PS 实际支持的协议以其 inventory `outputProtocols` 为准，前端下拉用它过滤。

## 4. API

### 4.1 加载协议 — `APIAddStorageProtocolMsg`
`POST /zstack/v1/primary-storage/protocol` `{uuid, outputProtocol}`
PS 的 `outputProtocols` 追加该协议，可重复添加（iSCSI + Vhost 共存）。添加后对该 PS 所有已连接主机
触发按协议主机准备（Vhost → zbsadm 部署 SPDK target），并写入 §2 protocol 表；主机准备失败不阻塞
API（下个 ping 周期自愈）。SDK `AddStorageProtocolAction`。

### 4.2 指定协议创建盘 — `APICreateDataVolumeMsg`
`POST /zstack/v1/volumes/data`，新增可选 `protocol`；传了必须同时传 `primaryStorageUuid`，
且 protocol ∈ 该 PS `outputProtocols`。协议来源优先级：API 参数 > systemTag
`ephemeral::volumeProtocol::{protocol}` > PS `defaultProtocol`。SDK `CreateDataVolumeAction`。

### 4.3 更改盘协议 — `APIChangeVolumeProtocolMsg`
`PUT /zstack/v1/volumes/{volumeUuid}/actions` `{changeVolumeProtocol:{protocol}}`
离线元数据变更：挂在 VM 上的卷要求 VM Stopped，下次启动用新协议挂载；Ready 态未挂载卷可直接切。
0 行更新视为失败（不谎报成功）。SDK `ChangeVolumeProtocolAction`。

### 4.4 查询按协议主机连通性 — `APIQueryExternalPrimaryStorageHostProtocolRefMsg`
`GET /zstack/v1/external-primary-storage/host-protocol-refs`
返回 `ExternalPrimaryStorageHostProtocolRefInventory`（hostUuid / primaryStorageUuid /
protocol / status / createDate / lastOpDate）。SDK `QueryExternalPrimaryStorageHostProtocolRefAction`。

### 4.5 VM 创建时经 systemTag 指定卷协议
`APICreateVmInstanceMsg` 无 per-盘 protocol 字段，经现成 systemTag 通道传入，tag 格式
`ephemeral::volumeProtocol::{protocol}`（`ephemeral::` 前缀必填）：
- `rootVolumeSystemTags` — 根盘
- `dataVolumeSystemTags` — 广播到所有数据盘
- `dataVolumeSystemTagsOnIndex` — 按 `dataDiskOfferingUuids` 下标（key `"0"`/`"1"`/…）精确指定

tag 建卷时被消费进 `VolumeVO.protocol`，因是 EphemeralPatternSystemTag 框架自动不落库。
enum 校验在 API 拦截器即做，非法协议当场拒绝（`unsupported volume protocol[...]`）。

### 校验错误文案
| 场景 | 报错关键句 |
|---|---|
| protocol 不在 PS 暴露集合 | `primary storage[uuid:...] does not expose output protocol[...]` |
| 传 protocol 但缺 primaryStorageUuid | `primaryStorageUuid is required when protocol is specified` |
| systemTag 协议非法 | `unsupported volume protocol[...]`（API 收到即拒） |
| 改协议但 VM 未停机 | VM state 校验错误（要求 Stopped） |
| 非 external PS | 仅 external PS 支持协议变更 |

## 5. 已知限制
- **懒重建**：宿主机/target 重启后运行中 VM 的 Vhost 盘需重启 VM 或手动 activate 才恢复，
  期间 I/O 挂起。升级/运维前建议迁移或关停 Vhost VM。
- **SPDK 镜像离线交付**：`Zbs.vhost.targetImage/targetImageTar/targetImageUrl` 三个来源默认全空，
  开箱需手工配内网 registry ref；ISO 随包离线交付为后续工作。
