# 主存储多协议 / 卷协议 — 前端对接文档

> 面向：前端 / UI 团队
> 后端分支：`feature-zbs-vhost`（zstack !10172）
> 本期口径：**三个 API 均为"接口预留"**——契约（API 名 / REST 路径 / 参数 / 校验语义）已冻结可对接，
> 但产品本期不承诺全量后端能力（见文末「实现状态总表」）。

## 1. 背景

外接主存储（Addon / vendor 类型，如华瑞 ZStone）一套存储可以同时暴露多种数据面协议。
例：一套华瑞可同时添加 **iSCSI** 和 **Vhost**。

- 协议的**默认实现由 ZStack 负责**：VM domain XML 的构建与管理（vhost-user / iSCSI 等盘的接线）全部在 ZStack 侧完成，vendor 只提供数据面。
- 卷（Volume）粒度记录自己的协议；同一主存储上不同卷可用不同协议。

## 2. 协议枚举（`VolumeProtocol`）

```
NVMEoF | iSCSI | Vhost | CBD | NBD | RBD
```

- 字符串严格区分大小写，传值必须与枚举一致（是 `NVMEoF` 不是 `Nvme`；是 `Vhost` 不是 `vhost`）。
- 一套 PS 实际支持哪些协议，**以该 PS inventory 的 `outputProtocols` 为准**（见 §3），前端下拉必须用它过滤，不要写死枚举全集。

## 3. 前端可查询的字段（现成，可直接用）

### 3.1 主存储（`ExternalPrimaryStorageInventory`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `outputProtocols` | List\<String\> | 该 PS 已添加的协议集合（驱动下拉/标签展示） |
| `defaultProtocol` | String | 不显式指定协议时新建卷的默认协议 |

查询：`QueryPrimaryStorage`（GET `/v1/primary-storage`），Addon 类型 PS 返回上述字段。

### 3.2 卷（`VolumeInventory`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `protocol` | String | 卷当前协议（列表页建议加列展示） |

### 3.3 按协议的主机连通性 — **已实现，可直接对接**

每台主机对每个协议的数据面连通性记录在**独立新表** `ExternalPrimaryStorageHostProtocolRefVO`（host × PS × protocol 一行，protocol **非空**，无 NULL 语义），通过专属查询 API 暴露：

**`APIQueryExternalPrimaryStorageHostProtocolRefMsg`**

```
GET /zstack/v1/external-primary-storage/host-protocol-refs?q=primaryStorageUuid=xxx
GET /zstack/v1/external-primary-storage/{primaryStorageUuid}/host-protocol-refs
```

返回 `inventories`（`ExternalPrimaryStorageHostProtocolRefInventory`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| hostUuid | String | 主机 UUID（支持 expand：`host.name=xxx` 条件） |
| primaryStorageUuid | String | 主存储 UUID |
| protocol | String | 协议（CBD / Vhost / ...，每个协议独立一行） |
| status | String | `Connected` / `Connecting` / `Disconnected`，**按协议独立** |
| createDate / lastOpDate | Timestamp | lastOpDate = 状态最近变化时间 |

SDK：`QueryExternalPrimaryStorageHostProtocolRefAction`。

**状态语义**：
1. 行的产生：主机连接/周期 ping 时按协议健康上报自动建行/刷新；`AddStorageProtocol` 成功准备主机后也会立即写入该协议的行。
2. **整机（host-level）状态保持历史折叠语义**（所有上报协议的 AND，任一协议不健康整机置 Disconnected），HA/调度/挂盘判定行为与升级前完全一致；**本表提供按协议的独立明细**——如 Vhost target 挂掉时，本表能精确显示 Vhost 行 Disconnected 而 CBD 行 Connected，定位是哪条数据面坏了。
3. 旧 `ExternalPrimaryStorageHostRefVO.protocol` 字段为历史遗留列（建表即有、无代码读写），按协议状态一律以本表为准。

**UI 建议**：PS 详情页「主机连通性」表格（列：主机 / 协议 / 状态 / 更新时间），按 hostUuid 分组多协议行展示。状态色：Connected 绿 / Connecting 黄 / Disconnected 红。

## 4. API 一：加载协议到主存储

**`APIAddStorageProtocolMsg`**（注意拼写：Protocol，产品侧笔记里 "APIAddStoragePotocol" 少了 r）

```
POST /zstack/v1/primary-storage/protocol
{
    "params": {
        "uuid": "fbfc898799694dfa9336d09690bd7e41",   // 主存储 UUID
        "outputProtocol": "Vhost"                      // 要添加的协议
    }
}
```

| 参数 | 必填 | 例子 | 备注 |
|---|---|---|---|
| uuid | 是 | xxx | 主存储 UUID |
| outputProtocol | 是 | `Vhost` | §2 枚举值 |

- 返回：`APIAddStorageProtocolEvent`（标准 async event，轮询 location）。
- 效果：PS 的 `outputProtocols` 集合追加该协议。
- 可重复调用添加多个协议（iSCSI + Vhost 共存）。
- SDK：`AddStorageProtocolAction`。
- **行为（已实现）**：添加协议后会对该 PS 所有已连接主机**触发按协议的主机准备**（Vhost → 自动部署 SPDK target），并把每台主机该协议的连通性写入 §3.3 的按协议状态表。主机准备失败不阻塞 API（协议已注册，下个 ping 周期自愈），UI 可在 §3.3 表格里观察每台主机的就绪状态。

## 5. API 二：指定协议创建盘

**`APICreateDataVolumeMsg`**（在现有创建数据卷 API 上新增可选参数）

```
POST /zstack/v1/volumes/data
{
    "params": {
        "name": "dv-1",
        "diskOfferingUuid": "...",
        "primaryStorageUuid": "fbfc8987...",   // 指定 protocol 时必填
        "protocol": "Vhost"                     // 可选；缺省走 PS defaultProtocol
    }
}
```

| 参数 | 必填 | 备注 |
|---|---|---|
| protocol | 否 | §2 枚举；**传了就必须同时传 `primaryStorageUuid`**，且协议必须 ∈ 该 PS `outputProtocols` |

- SDK：`CreateDataVolumeAction` 已带 `protocol` 字段。
- **协议来源优先级（设计约定，前端按此渲染表单逻辑）**：
  1. API `protocol` 参数（最高）
  2. systemTag `ephemeral::volumeProtocol::{protocol}`（**已实现**，见 §5.1）
  3. `DiskOfferingVO.protocol`（**已取消**，产品决定不做）
  4. PS `defaultProtocol`（兜底）

  本独立建卷 API（`APICreateDataVolumeMsg`）四档里实际生效的是 1、2、4：优先用 `protocol` 参数，缺省时回落到 `systemTags` 里的 `ephemeral::volumeProtocol::{protocol}`，再缺省走 PS defaultProtocol。两条通道共用同一套校验（enum + PS 暴露集合）。

### 5.1 VM 创建时按 systemTag 指定卷协议 — **已实现**

Cloud 建 VM 走 `APICreateVmInstanceMsg`，数据盘由 `dataDiskOfferingUuids` 描述，**没有 per-盘的 protocol 字段**。要在建 VM 时给数据盘指定协议，只能通过 systemTag：

`APICreateVmInstanceMsg` 的两个现成 systemTag 通道（已有字段，无需新增）：

| 字段 | 类型 | 作用域 | 用法 |
|---|---|---|---|
| `rootVolumeSystemTags` | List\<String\> | **根盘** | 给根盘指定协议时 |
| `dataVolumeSystemTags` | List\<String\> | **广播**到所有数据盘 | 全部数据盘用同一协议时 |
| `dataVolumeSystemTagsOnIndex` | Map\<String, List\<String\>\> | **按 `dataDiskOfferingUuids` 下标精确指定** | 不同数据盘用不同协议时，key = `"0"`/`"1"`/...（与 `dataDiskOfferingUuids` 同序） |

tag 格式（**注意 `ephemeral::` 前缀必填**）：`ephemeral::volumeProtocol::{protocol}`，protocol ∈ §2 枚举。前缀是后端框架的「临时 tag」约定——带前缀的 tag 在建卷时被消费进 `VolumeVO.protocol` 后由框架自动丢弃、绝不落库；漏掉前缀则不被识别（既不消费也不报错，等于没传）。

**例：两块数据盘，盘 0 用 Vhost、盘 1 用 CBD**
```
POST /zstack/v1/vm-instances
{
    "params": {
        "name": "vm-1",
        "instanceOfferingUuid": "...",
        "imageUuid": "...",
        "l3NetworkUuids": ["..."],
        "primaryStorageUuidForRootVolume": "fbfc8987...",
        "dataDiskOfferingUuids": ["offering-A", "offering-B"],
        "dataVolumeSystemTagsOnIndex": {
            "0": ["ephemeral::volumeProtocol::Vhost"],
            "1": ["ephemeral::volumeProtocol::CBD"]
        }
    }
}
```
全盘统一协议则改用 `"dataVolumeSystemTags": ["ephemeral::volumeProtocol::Vhost"]`。

**语义**：
- tag 在建卷时被**消费**——协议写进 `VolumeVO.protocol`；因是 ephemeral tag，框架**自动不落库**（`createNonInherentSystemTags` 跳过 ephemeral），卷上查不到该 tag，只查 `protocol` 字段。
- **enum 校验在 API 收到即做**（`APICreateVmInstanceMsg` 拦截器，三通道 `rootVolumeSystemTags` / `dataVolumeSystemTags` / `dataVolumeSystemTagsOnIndex` 全扫），非法协议（如 `ephemeral::volumeProtocol::BOGUS`）**当场拒绝**，报错见 §7；不会建出一个协议非法的卷。
- 同一盘若 API 协议参数与 tag 同时存在，API 参数优先（§5 优先级 1 > 2）；但 VM 创建链路本就没有 per-盘 API 协议参数，所以 VM 场景 tag 即唯一来源。
- **注意**：独立 `APICreateDataVolumeMsg`（§5）同样消费本 tag——缺省 `protocol` 参数时回落到 systemTag 里的协议（两通道共用一套消费逻辑，ephemeral 同样不落库）。`protocol` 参数与 tag 并存时参数优先（§5 优先级 1 > 2）。

## 6. API 三：更改盘的协议

**`APIChangeVolumeProtocolMsg`**（action 式）

```
PUT /zstack/v1/volumes/{volumeUuid}/actions
{
    "changeVolumeProtocol": {
        "protocol": "Vhost"
    }
}
```

| 参数 | 必填 | 备注 |
|---|---|---|
| volumeUuid | 是 | path 参数 |
| protocol | 是 | 目标协议，§2 枚举 |

- 返回：`APIChangeVolumeProtocolEvent`，inventory 里带更新后的卷（`protocol` 已翻转）。
- SDK：`ChangeVolumeProtocolAction`。
- **离线变更语义**：仅更新元数据；**挂载在 VM 上的卷要求 VM 处于 Stopped**，下次启动 VM 时用新协议挂载。未挂载（Ready 态）的卷可直接切。
- **UI 门控**：卷挂在非 Stopped 的 VM 上时，"更改协议"入口置灰，tooltip 提示"需要先停止云主机"。

## 7. 校验规则与错误文案（真机实测）

| 场景 | 后端报错（details 关键句） |
|---|---|
| protocol 不在 PS 暴露集合 | `primary storage[uuid:...] does not expose output protocol[NBD]` |
| 创建时传 protocol 但没传 primaryStorageUuid | `primaryStorageUuid is required when protocol is specified` |
| protocol 不是合法枚举 | invalid protocol 类校验错误 |
| 建 VM 时 systemTag 协议非法（`ephemeral::volumeProtocol::BOGUS`，含 `rootVolumeSystemTags`） | `unsupported volume protocol[BOGUS]`（API 收到即拒，不建卷） |
| 独立建卷 systemTag 协议非法（`ephemeral::volumeProtocol::BOGUS`） | `unsupported volume protocol[BOGUS]`（API 收到即拒，不建卷） |
| 改协议但 VM 没停机 | VM state 校验错误（要求 Stopped） |
| 卷不在外接（Addon）PS 上 | 仅 external PS 支持协议变更 |

## 8. UI 建议

1. **主存储详情页**：协议 tab / 区块——展示 `outputProtocols` 标签 + `defaultProtocol` 标记 + 「添加协议」按钮（下拉 = 枚举全集 − 已添加集合）。
2. **创建数据卷表单**：选了 Addon 类型 PS 后出现「协议」下拉（选项 = 该 PS `outputProtocols`，默认选中 `defaultProtocol`）；未选 PS 时隐藏该下拉。
3. **卷列表/详情**：加「协议」列；详情页操作菜单加「更改协议」（门控见 §6）。
4. **变更协议二次确认**：文案需包含"停机变更、下次启动生效"。

## 9. 实现状态总表

| 能力 | API 契约 | 后端实现 | 备注 |
|---|---|---|---|
| AddStorageProtocol | ✅ 冻结 | ✅ 完整（注册协议+触发按协议主机准备+写连通性行） | |
| CreateDataVolume.protocol | ✅ 冻结 | ✅ 已实现+真机验证 | |
| ChangeVolumeProtocol | ✅ 冻结 | ✅ 已实现+真机验证（CBD↔Vhost 双向） | |
| per-protocol 主机连通性 + 查询 API | ✅ 冻结（§3.3） | ✅ 新表 + QueryExternalPrimaryStorageHostProtocolRef | 整机状态保持折叠语义；按协议明细独立可查 |
| systemTag 指定协议（建 VM + 独立建卷） | ✅ 冻结（§5.1） | ✅ 已实现+IT 验证 | `ephemeral::volumeProtocol::{protocol}`（EphemeralPatternSystemTag）。建 VM 经 `rootVolumeSystemTags` / `dataVolumeSystemTags` / `dataVolumeSystemTagsOnIndex` 传入；独立建卷经 `systemTags` 传入。建卷时读进 `VolumeVO.protocol`，框架自动不落库。`protocol` 参数与 tag 并存时参数优先 |
| DiskOfferingVO.protocol | 已取消 | — | 产品决定不做 |

> 联调环境：feature-zbs-vhost 分支构建的 MN（参考 env1 172.24.191.9 / env2 172.24.248.246 部署），三个 API 均可真实调通。
