# AI 模型缓存存储层改进方案

## 背景

`feature-5.5.28-aios` 当前已经有一套 Host Model Cache 控制面，核心对象包括：

- `AiHostModelCacheVO`：按 `hostUuid + sourceRoot + sourcePath/modelUuid` 记录某台物理机上的模型缓存状态。
- `AiHostCacheStorageVO`：按 `hostUuid + sourceRootIdentity` 记录某台物理机某个缓存根目录的容量、可用容量、水位和健康状态。
- `AiHostModelCachePolicyVO`：按 `hostUuid + sourceRootIdentity` 配置缓存容量上限、水位和启停策略。
- `AiHostModelCacheReservationVO`：记录模型缓存预留和清理 claim。
- `AiHostModelCachePlanner`：在创建 VM 推理服务时优先选择已有 Ready cache 的 host，其次选择有足够 `effectiveAvailableBytes` 的 host。
- KVM agent 通过 `HOST_MODEL_CACHE_REPORT_PATH / PREPARE_PATH / CLEANUP_PATH` 上报、准备和清理 host model cache。

这套逻辑本质是 **host 维度的模型缓存与容量调度**。它适合解决“某台 host 是否已经有模型缓存，是否有足够空间继续缓存”的问题。

但实际模型推理场景里，模型文件来源和缓存层次至少有三层：

1. 远端模型源：JuiceFS、对象存储、NFS、模型中心等。
2. 主存储上的模型源缓存：按同模型 + 同主存储共享的只读模型目录，节约模型容量。
3. Host 侧远端读取缓存：如 JuiceFS cache-dir，用于加速远端读取，避免落到系统盘。

如果仍然只用当前 host cache 抽象承载所有语义，会出现几个问题：

- JuiceFS cache 默认放系统盘时，几百 GB 模型会快速打爆根盘。
- “host 能访问远端模型源”和“host 能访问已经缓存到主存储的模型目录”是两类不同能力，当前没有显式区分。
- 从容量节约角度，模型源应该按 `model + primaryStorage` 去重，而不是每个 host 都落一份完整模型。
- 从运行稳定性角度，JuiceFS cache-dir 又不应该跨 host 共享同一个目录，因为无法确认 JuiceFS 对共享 cache 目录的并发语义和淘汰行为。

## 目标

本方案目标是把“模型源缓存”和“远端读取缓存”拆开，形成清晰的存储层能力：

- VM 不需要直接访问存储网络，不需要对象存储/JuiceFS/Ceph 凭据。
- 同模型 + 同主存储只保存一份只读模型目录，降低主存储容量消耗。
- JuiceFS 或对象存储的 host 侧缓存不落系统盘，并具备容量管理和水位清理。
- 调度时能区分远端可访问、主存储已缓存、host 可见、runtime cache 健康等状态。
- 保留现有 `AiHostModelCache*` 的 host 层调度/引用计数能力，避免一次性推翻。

## 术语分层

### 1. Model Remote Source

远端模型源，表示模型文件最初来自哪里。

示例：

- JuiceFS metadata + object/NFS data backend
- S3/OBS/COS 对象存储
- NFS/CephFS 远端目录
- 模型中心已有下载源

它回答的问题是：

```text
某台 host 是否能访问远端模型源？
```

建议状态名：

```text
remoteAccessible
remoteType: JuiceFS | ObjectStorage | NFS | CephFS | ...
lastCheckTime
lastFailure
```

### 2. Model Storage Cache

主存储上的模型源缓存，语义是“同模型 + 同主存储”只读共享目录。

建议路径语义：

```text
/model-store/<primaryStorageUuid>/<modelUuid>/<contentVersion>/
```

它回答的问题是：

```text
某个模型是否已经在某个主存储上准备好？
```

这个层是容量节约的主要目标。

### 3. Host Model Visibility

某台 host 是否能访问某个主存储上的模型缓存目录。

对于 NFS/CephFS 类共享文件系统，多个 host 可能天然可见；对于 RBD/SharedBlock/LocalStorage，则需要更具体的 host mount/map 过程。

它回答的问题是：

```text
某台 host 是否能把主存储模型缓存目录只读透传给 VM？
```

建议状态名：

```text
cachedAccessible
readonlyMountPath
visibilityStatus
lastCheckTime
lastFailure
```

### 4. Host Remote Read Cache

host 侧远端读取缓存，例如 JuiceFS `--cache-dir`。

它回答的问题是：

```text
host 从远端模型源读取时，本地缓存目录在哪里、容量是否健康？
```

这一层应该按 host 隔离，不作为模型源容量去重的主目标。

建议路径：

```text
/var/lib/zstack/ai-model-cache/remote/<hostUuid>/<sourceIdentity>/
```

或者由主存储/本地数据盘提供：

```text
/data/zstack/ai-model-cache/remote/<hostUuid>/<sourceIdentity>/
```

## 当前实现与目标模型的差距

### 当前实现

当前 `AiHostModelCacheVO` 的唯一约束是：

```text
hostUuid + identityHash
```

`identityHash` 构造依赖：

```text
hostUuid + sourceRoot + sourcePath + size/mtime/checksum/contentVersion
```

`AiHostCacheStorageVO` 和 `AiHostModelCachePolicyVO` 也是：

```text
hostUuid + sourceRootIdentity
```

因此当前模型天然是 host 级：

```text
host -> sourceRoot -> sourcePath
```

### 需要补齐的抽象

新增主存储维度后，需要拆出：

```text
primaryStorageUuid + modelUuid + contentVersion
```

这一层不应该被 `hostUuid` 绑定。host 只是消费或暴露这个缓存目录。

建议目标关系：

```text
ModelStorageCache
  key: primaryStorageUuid + modelUuid + contentVersion
  status: Missing | Preparing | Ready | Failed | Deleting
  readonlyPath/exportPath
  sizeBytes/checksum/contentVersion
  refCount/lastUsedTime

ModelHostVisibility
  key: hostUuid + modelStorageCacheUuid
  status: Unknown | Accessible | Inaccessible | Mounting | Failed
  readonlyMountPath
  lastCheckTime/failure

HostRemoteReadCache
  key: hostUuid + remoteSourceIdentity
  sourceType: JuiceFS/Object/NFS/...
  cacheDir
  capacity/watermark/status
```

现有 `AiHostModelCacheVO` 可以在过渡期继续承担 `ModelHostVisibility + running ref` 的一部分职责，但长期建议不要把主存储模型源缓存继续塞进 host cache 表里。

## 主存储适配策略

不同主存储提供“模型源缓存目录”的方式不同。

| 主存储类型 | 建议实现 | 备注 |
| --- | --- | --- |
| NFS | 在主存储目录下创建只读模型目录 | 多 host 可见，最适合第一阶段 |
| CephFS | 挂载 CephFS 子目录作为模型目录 | 类似 NFS，需处理认证和 mount |
| LocalStorage | 每 host 本地一份模型目录 | 不具备跨 host 去重；迁移后 cache miss |
| RBD/Ceph Block | 创建 cache volume，map 到 host，mkfs，mount 成目录 | 注意单 host 挂载语义；不能多 host 同时 rw mount |
| SharedBlock | 创建块设备/卷后挂载目录 | 同样要明确单 host 独占或只读多 host策略 |
| External/第三方 | 通过扩展点返回可读目录能力 | 需要 capability 描述 |

建议引入主存储能力接口：

```java
interface AiModelStorageCacheBackend {
    boolean support(String primaryStorageType);

    ModelStorageCacheSpec allocate(ModelVO model, PrimaryStorageInventory ps);

    void materialize(ModelStorageCacheSpec spec, ModelRemoteSource source);

    HostVisibility prepareHostVisibility(String hostUuid, ModelStorageCacheSpec spec);

    void markReadonly(ModelStorageCacheSpec spec);

    void cleanup(ModelStorageCacheSpec spec);

    CapacityReport reportCapacity(String primaryStorageUuid);
}
```

第一阶段不需要所有主存储都完整实现。可以先支持：

1. NFS / SharedMountPoint 类文件系统主存储。
2. LocalStorage 作为退化模式。
3. RBD/SharedBlock 先作为设计预留，后续实现 cache volume 生命周期。

## 可访问性状态拆分

调度不能只判断一个 `hostAccessible`。至少拆成：

### remoteAccessible

host 能否访问远端模型源。

示例：

```text
host 可以访问 JuiceFS metadata/object backend
host 可以访问对象存储 endpoint
host 有必要网络/凭据/客户端
```

### storageCacheReady

模型是否已经在某个主存储上 materialize 成只读目录。

```text
primaryStorageUuid + modelUuid + contentVersion -> Ready
```

### cachedAccessible

host 是否能访问该主存储缓存目录。

```text
hostUuid + modelStorageCacheUuid -> Accessible
```

### remoteReadCacheHealthy

host 侧远端读取 cache-dir 是否健康。

```text
hostUuid + remoteSourceIdentity -> Healthy
```

建议启动判断顺序：

```text
1. 查询 ModelStorageCache 是否 Ready
2. 查询目标 host 是否 cachedAccessible
3. 如果 Ready + Accessible，直接用主存储只读目录 virtiofs 给 VM
4. 如果未 Ready，但 remoteAccessible=true，触发 materialize 到主存储
5. 如果 remoteAccessible=false 但已有 Ready + Accessible，仍允许启动
6. 如果都不满足，调度失败或等待异步缓存准备
```

## 与 virtiofs 的关系

VM 内建议暴露两个路径：

```text
/mnt/models       只读，模型源目录
/mnt/model-cache  可写，实例 runtime cache
```

其中：

- `/mnt/models` 来自 `ModelStorageCache` 或远端源的 host-side prepared path，应该只读。
- `/mnt/model-cache` 是 vLLM/HF/编译缓存等 runtime cache，可按实例隔离。

对于 JuiceFS 模型源，推荐：

```text
JuiceFS remote source
  -> host 侧 mount/prewarm
  -> host 侧 remote read cache-dir 放到非系统盘
  -> 主存储 ModelStorageCache 形成只读模型目录
  -> virtiofs readonly 给 VM
```

不要把 JuiceFS `cache-dir` 直接作为模型源共享语义。JuiceFS cache 是读取加速实现细节，不应承担“同模型 + 同主存储”去重职责。

## 容量管理

### ModelStorageCache 容量

容量由主存储管理。

需要记录：

```text
primaryStorageUuid
modelUuid
contentVersion
sizeBytes
reservedBytes
usedBytes
refCount
lastUsedTime
status
```

清理策略：

```text
1. refCount > 0 不清
2. Preparing/Ready 有独立状态转换
3. Failed/Unknown 可优先清理
4. Ready 且 lastUsedTime 最旧的可按 LRU 清理
5. 清理前检查是否仍有 VM mount/ref
```

### HostRemoteReadCache 容量

容量由 host 管理，不占系统盘。

规则：

```text
1. cache-dir 必须可配置
2. 默认不能落到 /
3. 启动前检查 cache-dir 所在 filesystem 是否为系统根盘
4. cache-size 按可用容量动态计算，不硬编码 800G
5. 支持 high/low watermark
6. 按 host 隔离，不跨 host 共享同一个 JuiceFS cache-dir
```

建议默认计算：

```text
cacheSize = min(configuredMax, availableBytes * 0.7)
freeSpaceRatio >= 0.03
```

如果发现 cache-dir 与 `/` 同 filesystem：

```text
默认拒绝启用，除非管理员显式覆盖。
```

## 调度策略改进

当前 `AiHostModelCachePlanner` 逻辑：

```text
Ready host cache hit -> 选该 host
否则 enough host cache storage -> 选有效容量最大的 host
否则按策略 fallback/fail
```

建议改为多层策略：

```text
1. Ready ModelStorageCache + cachedAccessible host
2. Ready ModelStorageCache + 可 prepare visibility 的 host
3. 未 Ready，但 remoteAccessible host + 主存储容量足够，可触发 materialize
4. fallback 到当前 host cache 策略
5. CacheRequired 时明确失败
```

选择 host 时，优先级建议：

```text
1. 已有 cachedAccessible 且 GPU/业务约束满足
2. 同 primaryStorage 已 Ready，host 可快速挂载
3. remoteAccessible 且 remoteReadCacheHealthy
4. effectiveAvailableBytes 最大
```

## 数据模型建议

### 新增 ModelStorageCacheVO

```text
uuid
primaryStorageUuid
modelUuid
modelCenterUuid
contentVersion
sourceIdentity
cachePath/exportPath
sizeBytes
checksum
status
refCount
lastUsedTime
failureCode
failureMessage
```

唯一键：

```text
primaryStorageUuid + modelUuid + contentVersion
```

### 新增 ModelStorageCacheHostRefVO

```text
uuid
cacheUuid
hostUuid
status
readonlyMountPath
lastCheckTime
failureMessage
```

唯一键：

```text
cacheUuid + hostUuid
```

### 新增 HostRemoteReadCacheVO 或扩展 AiHostCacheStorageVO

如果继续沿用 `AiHostCacheStorageVO`，需要明确它表示的是 host 侧 runtime/remote read cache，而不是主存储模型源缓存。

建议字段补充：

```text
sourceType: JuiceFS | ObjectStorage | NFS | ...
cachePurpose: RemoteReadCache | RuntimeCache
filesystemId/deviceId
isRootFilesystem
```

## 与现有表的兼容

短期可保留现有表：

- `AiHostModelCacheVO`：继续记录 host 侧实际 attach/running ref，以及旧逻辑的 host cache。
- `AiHostCacheStorageVO`：继续记录 host cache root 容量和 policy。
- `VmModelMountVO.cacheUuid`：过渡期仍可指向 `AiHostModelCacheVO`。

新增主存储模型源缓存后，建议在 `VmModelMountVO` 或新关联表中记录：

```text
modelStorageCacheUuid
hostVisibilityUuid
```

避免未来把 `cacheUuid` 同时解释为 host cache 和 primary storage cache。

## 关键流程

### 模型预热 / 缓存准备

```text
1. 用户创建/导入模型
2. 选择目标 primaryStorage 或按服务部署目标推导 primaryStorage
3. 创建 ModelStorageCache(status=Preparing)
4. 如果 host 能 remoteAccessible，则通过 host 访问远端源
5. host 侧使用 HostRemoteReadCache 加速读取
6. 将模型 materialize 到主存储缓存目录
7. 校验 size/checksum/contentVersion
8. 标记 ModelStorageCache Ready
9. 标记相关 host visibility Accessible
```

### VM 启动

```text
1. ModelServiceBackend 调用 planner
2. planner 查询 ModelStorageCache + HostVisibility + HostRemoteReadCache
3. 选出 host
4. VmModelMountManager 构造 virtiofs sourcePath
5. KVM agent attach readonly virtiofs
6. VM 内看到 /mnt/models
```

### 远端源不可访问但缓存已存在

```text
remoteAccessible=false
storageCacheReady=true
cachedAccessible=true
```

这种情况下应该允许启动。因为 VM 只需要 host 可读的模型目录，不需要远端源。

### 缓存不存在但远端源可访问

```text
remoteAccessible=true
storageCacheReady=false
```

可触发异步缓存准备或同步等待，取决于服务启动策略。

### 两者都不可用

```text
remoteAccessible=false
storageCacheReady=false
```

调度失败，错误信息应明确：

```text
host cannot access remote model source and model is not cached on selected primary storage
```

## 验证清单

### 0. 空逻辑 / 兼容性 case

先验证当前代码路径在未启用完整主存储缓存生命周期时不破坏现有行为。

- [ ] 未配置 `ModelService.primaryStorageUuid` 时，仍使用原 ModelCenter prepared path。
- [ ] 配置 `ModelService.primaryStorageUuid` 时，模型源路径切到主存储缓存目录：

  ```text
  <primaryStorage.mountPath>/ai-model-cache/models/<modelUuid>/<contentVersion>
  ```

- [ ] `contentVersion` 依次从 `artifactChecksum`、`versionSemver`、`version`、`default` 推导，路径字符安全。
- [ ] `AiHostModelCacheVO` 仍可记录 host 侧 visibility / running ref，不要求新增表即可跑通现有空逻辑。
- [ ] `VmModelServiceBackend` 构造 virtiofs source path 时与 planner 使用同一套主存储路径规则。
- [ ] 现有 fallback / cacheRequired 语义不变：主存储缓存不可用时，不误判成已有 Ready cache。
- [ ] 运行 `./runMavenProfile premium` 编译通过。

### 1. NFS / SharedMountPoint 主存储

第一批优先验证文件型共享主存储，因为它最接近目标模型。

- [ ] 主存储目录下能创建 `ai-model-cache/models/<modelUuid>/<contentVersion>`。
- [ ] 多台 host 能看到同一个只读模型目录。
- [ ] VM 通过 virtiofs 只读挂载 `/mnt/models` 后可读取模型文件。
- [ ] 同一模型在同一主存储只保留一份缓存目录。
- [ ] 多个 VM 复用同一模型缓存时，host ref / mount ref 计数正确。
- [ ] VM 删除或服务停止后，不清理仍被其他 VM 引用的模型目录。
- [ ] 模型目录被手工删除或不可读时，调度能给出明确失败原因。

### 2. CephFS 主存储

CephFS 语义接近 NFS，但需要额外验证认证、挂载和目录权限。

- [ ] host 具备 CephFS 访问凭据时可创建并读取模型缓存目录。
- [ ] 缓存目录 readonly 暴露给 VM，VM 不能修改模型源。
- [ ] host 丢失 CephFS mount 或认证失败时，`cachedAccessible` 能变为失败态。
- [ ] 多 host 并发读取同一模型目录无一致性问题。
- [ ] 容量统计来自主存储维度，而不是 host 系统盘。

### 3. LocalStorage 主存储

LocalStorage 作为退化模式验证，不要求跨 host 去重。

- [ ] 模型缓存目录落在目标 host 的 LocalStorage 路径下。
- [ ] 同 host 上多个 VM 可复用同一模型目录。
- [ ] 不同 host 上同一模型允许各自 materialize 一份缓存。
- [ ] VM 迁移到其他 host 后，允许 cache miss 并重新准备缓存。
- [ ] LocalStorage 容量不足时，调度失败信息指向目标主存储容量不足。

### 4. RBD / Ceph Block 主存储

块存储不能直接作为 virtiofs source，必须验证 cache volume 生命周期。

- [ ] 为模型缓存创建独立 cache volume。
- [ ] host 可 map block device、mkfs、mount 成目录后再作为 virtiofs source。
- [ ] cache volume 不被多 host 同时 rw mount。
- [ ] 只读暴露给 VM 前，模型文件已完成写入和 checksum 校验。
- [ ] VM 停止后，cache volume refCount 正确下降但 Ready cache 不被误删。
- [ ] host 异常重启后，可恢复 map/mount 状态或进入明确 Failed 状态。

### 5. SharedBlock 主存储

SharedBlock 重点验证独占挂载和只读复用边界。

- [ ] 创建 cache volume 后能在目标 host mount 成目录。
- [ ] 同一 cache volume 的 rw 准备阶段有互斥保护。
- [ ] Ready 后只读复用策略清晰：单 host 只读复用或多 host 只读挂载必须有明确实现。
- [ ] 并发 VM 启停不会导致 block device 被提前 unmap。
- [ ] 清理 cache volume 前确认无 VM mount/ref。

### 6. Host Remote Read Cache

这一层独立验证，不把它当作主存储模型源缓存。

- [ ] JuiceFS / Object / NFS 远端读取 cache-dir 默认不落 `/` 所在 filesystem。
- [ ] cache-dir 所在 filesystem、容量、设备标识能被 agent 上报。
- [ ] cache-size 按配置上限和可用容量计算，不硬编码。
- [ ] high / low watermark 清理不影响主存储上的 readonly 模型源缓存。
- [ ] 不同 host 不共享同一个 remote read cache-dir。

## 实施建议

### Phase 1：明确语义，不大改现有行为

- 文档和 API 层明确区分：
  - host model cache
  - primary storage model cache
  - remote read cache
- 在现有 `AiHostCacheStorageVO` telemetry 中增加是否根盘、filesystem identity 等信息。
- 禁止 JuiceFS cache-dir 默认落系统盘。
- agent 上报 cache-dir 所在 filesystem 容量和设备标识。

### Phase 2：引入主存储模型缓存

- 新增 `ModelStorageCacheVO` 和 `ModelStorageCacheHostRefVO`。
- 先支持 NFS/SharedMountPoint/LocalStorage。
- 调度优先使用 `ModelStorageCache Ready + HostVisibility Accessible`。

### Phase 3：主存储后端扩展

- 支持 RBD/SharedBlock cache volume。
- 实现 cache volume 生命周期、mount、readonly export。
- 完成清理、引用计数和失败恢复。

### Phase 4：统一容量和清理

- 按主存储维度管理模型源缓存容量。
- 按 host 维度管理 remote read cache 容量。
- 提供运维 API 和告警。

## 风险与约束

- RBD/block 不能直接作为 virtiofs 后端，必须先在 host 上 map/mkfs/mount 成目录。
- 普通文件系统不能多 host 同时读写同一块设备缓存目录。
- JuiceFS cache-dir 不建议跨 host 共享同一目录。
- 主存储模型缓存目录应只读透传给 VM，避免 VM 修改模型源。
- runtime cache 与模型源 cache 必须分开，否则清理策略会互相污染。
- LocalStorage 模式不能跨 host 去重，迁移后允许 cache miss。

## 推荐结论

最终设计应采用两层缓存：

```text
模型源缓存：按 modelUuid + primaryStorageUuid + contentVersion 去重，readonly，节约容量。
远端读取缓存：按 hostUuid + remoteSourceIdentity 隔离，read/write，提升远端读取性能，不落系统盘。
```

调度判断必须区分：

```text
remoteAccessible
storageCacheReady
cachedAccessible
remoteReadCacheHealthy
```

这样既能满足“VM 不接入存储网络”的需求，也能避免系统盘被 JuiceFS cache 打爆，同时保留同模型同主存储只存一份的容量优势。
