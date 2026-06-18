# ZBS vhost — Next Session Handoff

## ✅ 2026-06-16 rebase 到 MR target + env3 全 e2e（含 AddStorageProtocol API）全绿

### rebase（修正版）
- MR **!10172** target = `upstream/feature-zbs-vhost`（zstackio，**不是 5.5.28**）。⚠️ 教训：第一次误 rebase 到
  `upstream/5.5.28`（把 24 个主线 commit 塞进来，MR 变成 25 commits + cannot_be_merged）。**MR rebase 基线永远用
  MR 的 target_branch**（`gl_get_merge_request` 看 `target_branch`），不是某个 release 分支。已回滚重做。
- 正确做法：`git rebase --onto upstream/feature-zbs-vhost a9bc5f4d8a feature-zbs-vhost`（0 冲突，我的 1 个
  feature commit 落到 target tip `9aa6eb5f4d` 之上）。结果单 commit `8a35a2b2d1`，保留 Change-Id `If240e24…`，
  `--force-with-lease` push origin。MR 现 `diverged_commits_count:0` / `source_commits_count:1` / changes 39。
- 备份 tag `backup/feature-zbs-vhost-v2`（旧 tip `cbf357a4a4`）。premium submodule 全程 `23a9c027cd` 不动。
- ⚠️ **SDK regen drift**：`runMavenProfile sdk` 会按本地 stale source-class-map 误删 upstream 的 License SDK
  文件（`GetLicenseAuthorizationInfoAction` 等，upstream commit `4d4096ad9a` 加的）。regen 后必须
  `git checkout HEAD -- sdk/ testlib/` 丢弃这些 drift，只保留 feature commit 里的 SDK 文件。
- 全量 premium build SUCCESS + `ZbsVhostVolumeCase` IT `Tests run:1 Failures:0 Errors:0`。

### env3 全 e2e（172.24.194.114，干净 rebuild jar）
| 用例 | 结果 |
|---|---|
| **AddStorageProtocol API**（本轮重点）：reset→CBD-only → `POST /primary-storage/protocol {outputProtocol:Vhost}` → `onProtocolAdded` → per-protocol 表 `ExternalPrimaryStorageHostProtocolRefVO` 自动建 3 行：248.21/193.182 **Connected**(docker，ensure target 成功+容器 Up)、249.25 **Disconnected**(无 docker)；REST 查询 API 也返回 3 行 | ✅ |
| Q1a 根盘 systemTag=Vhost→protocol=Vhost / Q1b 根盘 NBD→拒（对称修复） | ✅ |
| Q2c/d NBD field/tag→拒 | ✅ |
| R1/R2/R3 显式建卷三连 / C1/C2 双向 changeVolumeProtocol | ✅ |

### env3 部署要点（与 MR 分支无关，env3 特有）
- env3 是 stock 5.5.28（比 feature 分支新），feature jar **缺** `ErrorCodeDiagnostic{,$RawMessage,Helper}` 3 类
  → 替包后必须从 env3 stock header jar `jar uf` 重注入这 3 类，否则 API error 路径 NoClassDefFoundError 500。
  （这是「环境比 feature 分支新」的部署兼容问题，**不是 MR 该改的**；MR 合进 feature→将来 feature 合 5.5.28 时自然拿到。）
- **`systemctl restart zstack-kvmagent` 必须带 `.service` 全名**，否则 SYSV restart 假成功不换进程 → vhost 端点 404。
- 部署清单 5 jar(header/storage/zbs/sdk/**tag**) + 3 xml(persistence.xml sed 插行) + property(registry ref) +
  DB(建表/删死列/插 Vhost outputProtocol) + 4 py(真重启 agent)。备份 `/root/vhost-deploy-backup-env3-20260616`。
- REST helper `/root/env3-api.sh`（异步 job poll）；测试脚本 `/root/{addproto,all,regr}-test.sh`。

---

## ⏭️ 下个 session 从这里开始（2026-06-11 更新）

**数据盘 vhost 真·可用性已证明 ✅**（见下「数据盘 vhost guest 级读写验证」）。剩余工作按优先级：

> **下个 session 3 大设计待办（同一 framework，一起设计；细节见各自正文/决策记录）**
> 1. ✅ **per-protocol HA slot/grain 表形态 = 路 B（已实现，2026-06-15）** —— 锁路 B（保留继承 + 独立明细表）。决定性依据：`ExternalHostIdGetter` 槽位分配的「一行=一槽」不变量（`steppingAllocate`/`randomAllocateHostId` count 行数判满），路 A 多行会让 hostId 重复→`used.size()` 虚高→槽位撞号→HA fencing 腐败。已落地：删子类死 `protocol` 列（VO+metamodel+`V5.5.28` DROP_COLUMN）；`checkHostAccessible` 协议化（volume.protocol!=null 读明细表，否则走折叠父表）。详见下「## 决策记录」。全量 premium build 绿。
> 2. ✅ **systemTag 传 volume protocol（已实现，2026-06-15，user 指正改用 EphemeralPatternSystemTag）**：Cloud 建 VM **不填 DiskAO**，protocol 走 `VolumeSystemTags.VOLUME_PROTOCOL`。**类型 = `EphemeralPatternSystemTag`**（`extends PatternedSystemTag` → 带 `{protocol}` token **又** ephemeral；wire = `ephemeral::volumeProtocol::{protocol}`）。「临时」语义由框架免费提供：`createNonInherentSystemTags`（createVolume 已调）自动跳过 ephemeral tag 不落库，**无需手动 strip**。`VolumeManagerImpl.createVolume` 用 `getSystemTag(::isMatch)`+`getTokenByTag` 读进 `VolumeVO.protocol`（仿同包 `REQUIRED_INSTALL_URL`）。多数据盘消歧复用既有 `dataVolumeSystemTagsOnIndex["N"]`（positional，零新 schema）；enum 校验在 `VolumeApiInterceptor.validate(APICreateVmInstanceMsg)` early-return 前。⚠️ 前端必须发 `ephemeral::` 前缀。全量 premium build 绿；IT 待重跑（改类型后）。
> 3. **host vhost target 部署时机 = `ExternalPrimaryStorageHostProtocolRefVO` 进 connecting 时（2026-06-15 user 精确化）**：per-protocol ref 行建立连接（AddStorageProtocol(Vhost)/PS attach cluster 触发）→ ensure_target on that host；失败暴露运维操作面；现 VM-start 懒加载降为幂等兜底。**路 B 已锁 → 落点 = 明细表行 connecting**（非去继承行）。⏳ 本 session 未做。

1. **MR review/合并**：zstack **!10172** = 单 commit `df19328484`（vhost lazy deploy + volume protocol create/change，volume-msg+PUT，coderabbit 两条已落地+已 resolve）；utility **!7256** = hugepage auto-size + lazy deploy 加固 `6c523c4cc`（coderabbit 5 条全修：shlex.quote 注入 / pull 失败落 tar+url fallback / target_healthy 三态(container_exists) / RLock 串行化 / insecure-registry 改 TLS 错误才降级重试；单测 30/30，`test_zbs_vhost_deploy.py` 新增 13 个）。都 target 上游 `feature-zbs-vhost`。⚠️ env1 主机上跑的还是旧版 .py，要复验加固行为需重推 agent 代码。
2. **收敛 reportNodeHealthy 健康语义**（allMatch → per-protocol，防 vhost target 死拖垮整机 CBD connected）。
3. **per-protocol 主机连接 = HA slot/grain 设计**（详见下「## 决策记录：per-protocol HA slot/grain（2026-06-15）」）。`ExternalPrimaryStorageHostRefVO.protocol` 死字段结论=**删**；per-protocol 状态去向（独立明细表 vs 切断继承让老表多行）**待最终拍板**。部署时机见顶部「3 大待办」第 3 条。与第 2 条 reportNodeHealthy per-protocol 同一 framework 改动。
3b. **changeVolumeProtocol 防护（升级为内容探测 gate）**：①根盘：已装 OS 512e↔4Kn 双向切换都变砖 → 拒或强警告。②数据盘 CBD→Vhost：**老版本未发 physical_block_size，存量卷 fs 多为 512 元数据（xfs sectsz=512/小 ext4 1K block/512-LBA 分区表），mkfs 时刻冻结、升级不改，切 Vhost 必砖且无救援**（vhost block size 来自 SPDK blkcfg 不可 override）→ agent 切换前读卷头探测：xfs sb@0 的 sb_sectsize / ext4 sb@1024 的 s_log_block_size / MBR@510 / GPT@512，512-flavor 即拒。存量卷出路：倒数据重格式化 / 等 ZBS 512e bdev / 留 CBD。
4. **清理测试残留**：vhost-livetest2-5（livetest5 已 Stopped，root 仍 Vhost）、`cbd-vhost-dvtest`（e21459f2，验证 VM，Running@190.207，挂 vhost-dvtest + cbd2vhost-test(70afb426,Vhost) + vhost2cbd-test(11c3f520,CBD)）、vhost-switch-test 卷、env1 上的 rework jar/GlobalProperty（备份 `/root/vhost-rework-backup-*`，serviceConfig API 注册已挪到 volume.xml）。
5. live-migration 目标机 `createWithFlags(0)` 不走 `Vm.start()` hook → hugepage 不自动扩（vhost VM 本就 offline-only，暂不修）。
6. **ISO 离线交付 SPDK 镜像（方案已定，待打包+小 Java 改动）**：通道半现成——`Zbs.vhost.targetImageTar/Url` GlobalProperty（ZbsGlobalProperty.java:36/38）+ cmd 透传（fillVhostTargetParams:201-202）+ agent load_image tar/url 兜底都在，**但默认值全空 = 开箱死路**：默认 `targetImage=zbs-vhost:latest` 无 registry 前缀（pull 分支跳过）+ tar/url 空 → load_image 直接抛 absent；env1/env2 能跑全靠手工配 registry ref。落地三件事：① 构建期 `docker save` tar.gz 进 MN RPM（落 webapps 静态路径，8080 天然 served）；② **fillVhostTargetParams 运行时推导 url**（property 空且本地交付 tar 存在 → 填 `http://{当前MN IP}:8080/zstack/zbs-vhost/<tar>`；注解 defaultValue 是编译期常量填不了 IP，安装期写死又怕 MN IP 变/多 MN）；③ `targetImage` 默认换成带 tag 的无-registry ref。升级=换 tar+改 tag。小清理：download_image 用完不删 tmp tar。**打包落点已定（2026-06-12）**：进 **bin**（MN 安装/升级包，ISO 装机走 bin 自动继承；只进 ISO 会导致升级环境镜像版本与 agent 代码漂移）。bin 内与 all_in_one.tgz 平级（不塞 war），install/upgrade 脚本拷到 `webapps/zbs-vhost/` **独立目录**（不放 webapps/zstack 里，避免升级重铺 war 误删；URL=`http://{MN IP}:8080/zbs-vhost/<tar>`）。文件名带 tag，fillVhostTargetParams 扫目录取名推导 url，文件不存在回退现状（registry/报错）。体积实测（2026-06-12 @246.239）：运行态 247MB / save tar 241MB / **save+gzip 83MB**（bin 实际增重），主 bin 直推无压力；体积不可接受时备选：拆独立 `zstack-zbs-vhost-image` 附加包进 ISO repo。调试走 /zstack-mn-bin-debug 解包重打包流程。下载链路索引：activateVhostVolume(ZbsStorageController.java:160)→fillVhostTargetParams(:196)→VHOST_ACTIVATE_PATH(:170)→zbs_storage_plugin.py:125→zbs_vhost_target.py:248 ensure_target→:198 load_image（image_present 短路=只第一次下）→**:99 docker pull 即下载本体**。

**env1 现状**：MN 跑 rework jar；`vhost-livetest5`(336967d0) Stopped；vhost 数据盘 `vhost-dvtest`(f510f28f) 已挂到 `cbd-vhost-dvtest`(e21459f2) 上且验证通过。

---

## 决策记录：per-protocol HA slot/grain（2026-06-15）

**触发**：user 质疑「老表 `ExternalPrimaryStorageHostRefVO` 没新增字段就没必要」+「分协议展示 ps-host 连通有没有别的办法弥补，之前设计有问题」。一路掘到 HA fencer 形态。

### 掘出来的硬事实（全部 code-verified，不是推测）
- **`hostId` 是 HA 心跳槽位号**，不是身份 ID。agent 端 `ha_plugin.py:1482` `offset = host_id × heartbeat_required_space` —— 每 host 按 hostId 算偏移往共享心跳卷写时间戳，别人读各自槽判生死（sanlock 风格 lease slot）。
- **ZBS 心跳卷 per-pool 且恒走 CBD**（`ZbsStorageController.activateHeartbeatVolume`：`CbdHeartbeatVolumeTO`，key=poolName）。**`KvmVhostNodeServer` 没有 self-fencer、不碰 hostId** → vhost 通路当前不参与 HA 心跳。
- **缺口**：vhost 容器死 → CBD 心跳照跳 → host 不自隔离 → vhost VM 卡 IO 且无 HA 接管。单一 CBD 心跳对 vhost 数据面是瞎的。
- **`getNodeSvc(psUuid)` 按 PS 取，不分协议**；一个 PS 一个 node service。

### slot 来源 = 两种策略，grain 跟来源走（核心顿悟）
| 策略 | 谁给号 | 天然 grain | 唯一性 |
|---|---|---|---|
| 存储派生（expon `getHostId()=uss.getServerNo()`，按协议查网关） | 存储侧网关 | per-(host,protocol) | 存储侧保证；**hazard**：两 host 同协议网关落同一 server 会撞号 |
| 自分配（ZBS `ExternalHostIdGetter` 1..999 池） | 我们自己叠 | 我们说了算 | 抽象池天然防撞 |

**user 拍板**：自分配时定义成**跨协议一样**（给定 (ps,host) 一个号，所有协议心跳介质复用它）。号是我们发的就有这个自由；expon 的 per-protocol 是因为号来自 per-protocol 网关、被动接受。→ ZBS 1..999 池看着 per-host 纯属单协议偶然（只有 CBD 一条路参与 HA，两种 grain 数值重合）。

### 三层分解（正确终态）
- **连通性 status** → per-(ps,host,protocol)。
- **心跳 slot/hostId** → 自分配统一放 per-(ps,host)；存储派生不存、现查。
- **父 `PrimaryStorageHostRefVO.status`** → 退化成折叠 rollup（折叠策略 any/all 待 checkHostAccessible 协议化后再定，届时不再 load-bearing）。
- **放置门禁 `VolumeApiInterceptor.checkHostAccessible`（:457）** → 必须协议化：vhost 卷查 vhost 路状态，不是折叠值（否则 vhost 容器死、CBD 活=折叠 Connected → 把 vhost VM 放上去卡死）。
- **roadmap**：加 vhost 独立心跳介质 + self-fencer + vhost-dir covering，复用 hostId。**不变量：一协议一心跳介质**（否则同槽号互相覆盖）。

### 表形态分歧（⏳ 待 user 最终拍板）
status 的真 grain = per-(ps,host,protocol)，要落 DB。两条路：

**路 A — 切断继承（user 倾向）**：`ExternalPrimaryStorageHostRefVO` 不再 extends `PrimaryStorageHostRefVO`，自己独立 → 天然能 per-(ps,host,protocol) 多行，复活那根死 `protocol` 列当判别列。
- 代价：外部存储**离开多态家族**，下列 **4 个父表消费者**切断后看不到外部行，全要加「外部走新表 + 自算折叠 rollup」：
  - `VolumeApiInterceptor:457` checkHostAccessible（放置门禁）
  - `KVMHost:5417` inaccessiblePsCount（**host HA**：漏改 → 外部存储失联不计入整机隔离，最危险）
  - `PrimaryStorageManagerImpl:1191`、`PrimaryStorageBase:543`
- 外部状态写路从「写父表」重定向到「写新表」；要写迁移（父+子 现存行搬进独立表）。

**路 B — 保留继承 + 独立明细表（assistant 倾向，= 已落地的现状增强）**：
- 父 `PrimaryStorageHostRefVO` 行**保留**当外部存储的**折叠 rollup**（per-(ps,host) 一行）+ 自分配 slot(hostId) 的家。host-HA / 粗放置免费继续用，零改。
- per-protocol status 放**独立非继承表 `ExternalPrimaryStorageHostProtocolRefVO`**（已建好/已部署/已测，就是它）。
- 删老 JOINED 子类那根死 `protocol` 列（子类瘦成只剩 hostId）。
- 只把 `checkHostAccessible` 协议化（读明细表）。

**核心分歧点**：折叠的「host↔PS 连通」事实 **host-HA 真需要**。路 A 把它踢出父家族后，那个 rollup 你最后还得自己造出来喂 host-HA；路 B 让父行天然当 rollup。assistant 据此倾向 B（折叠行非垃圾，是 host-HA 合法消费的 rollup）。user 倾向 A（单张表更干净）。

**✅ 最终拍板：路 B（2026-06-15 实现）**。决定性新事实（不在上面论证里）：`hostId` 槽位分配器的「一行=一槽」不变量——`ExternalHostIdGetter.steppingAllocate/randomAllocateHostId` 全靠 `select(hostId).listValues().size() == 行数` 判池满。路 A 让老表 per-(ps,host,protocol) 多行 → 同 hostId 在 N 个协议行里重复 → `used.size()` 虚高 → 槽位分配错乱 → 心跳 offset 撞号（`ha_plugin.py:1482 offset=host_id×space`）= HA fencing 数据腐败。且即便纯 A，hostId 仍要自己的 per-(ps,host) 家 → 还是两张表。路 B 用已落地明细表 + 父行天然当 rollup，14+ 父表消费者零改、无迁移。落地：删子类死 `protocol` 列（VO+metamodel+`V5.5.28 DROP_COLUMN`）；`checkHostAccessible` 协议化（`volume.protocol!=null` → 读明细表，否则走折叠父行不变）。

> 注：item 2（reportNodeHealthy per-protocol）+ item 3（本决策）+ vhost self-fencer 是同一个 framework 改动，一起设计。当前已落地的「全家桶」是路 B 的雏形（明细表只装 status、未接 checkHostAccessible 协议化、未接 vhost fencer）。

### systemTag 传 volume protocol（2026-06-15 实现，路 B 配套）
- Cloud 建 VM 不填 DiskAO → 卷 protocol 只能走 systemTag。`VolumeSystemTags.VOLUME_PROTOCOL` = **`EphemeralPatternSystemTag`**（wire 形 `ephemeral::volumeProtocol::{protocol}`）。**关键纠正（user 指出）**：`EphemeralPatternSystemTag extends PatternedSystemTag`，**既带 `{protocol}` token 又是 ephemeral**——之前「EphemeralSystemTag 不能带 token」判断错了，错用了裸 PatternedSystemTag + 手动 strip。同包先例 `ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL`。详见 memory [[reference_ephemeral_pattern_systemtag]]。
- **消费点 = `VolumeManagerImpl.createVolume(CreateVolumeMsg)`**（所有 VM-create 卷的汇聚点，VmAllocateVolumeFlow 把 tag 折进每卷 CreateVolumeMsg.systemTags）：gate `vo.getProtocol()==null`（显式 msg.protocol 优先）→ `getSystemTag(VOLUME_PROTOCOL::isMatch)` 读 token + setProtocol。**不手动 strip**——`createNonInherentSystemTags`（createVolume:634 已调）对 ephemeral tag 自动跳过落库（TagManagerImpl:366），框架天然实现「落地即不常驻」。
- **多盘消歧 = 复用现成 `dataVolumeSystemTagsOnIndex["N"]`**（VmAllocateVolumeFlow:72-78 按位置 setTags 到对应 VolumeSpec），裸 tag 进 `dataVolumeSystemTags` 则广播全数据盘。零新 tag schema。
- **enum 守门 = `VolumeApiInterceptor.validate(APICreateVmInstanceMsg)` 前置**（在 getDiskAOs() 早返回之前，读 dataVolumeSystemTags + OnIndex）：抄 validateVolumeProtocol:497 的 enum-known 检查 argerr 拒未知协议。PS-exposes-protocol 在 API 期查不了（PS 流程中段才分配），enum 是 fail-fast 层。

### ⚠️ 发现的潜在 product bug（本次未修，out of scope）
`VolumeBase.expunge`（:1177）只要 `primaryStorageUuid != null` 就跑 delete-on-PS flow，**无 NotInstantiated/installPath 守门**（对比 `delete` 用 `allowEmptyFlow()`）。外部 PS 上一个 NotInstantiated 卷（installPath=null）+ protocol≠null 被 expunge → `ExternalPrimaryStorage.deactivateAndDeleteVolume(null, protocol)` 跳过 null-guard → `node.getActiveClients(null, protocol)` NPE。正常流程卷在 expunge 前已 instantiate（有真 installPath），故少见；但「建数据卷指定 protocol→从不 attach→删→expunge」路径理论可触发。修法：expunge 的 delete-on-PS flow 加 `skip(){ return self.getInstallPath()==null }`。

---

## 已完成 / 已验证

### per-protocol 主机连通性全家桶（2026-06-12）✅ 实现+IT+双环境真机
设计：父表 UNIQUE(ps,host) 索引（V4.3.12）封死同表多行 → **新表 `ExternalPrimaryStorageHostProtocolRefVO`**（ps,host,protocol,status，UNIQUE 三键，FK cascade），14 个 legacy 读点零改动。host-level 行语义改为**默认协议健康**。
- **健康记录（按用户要求收敛为最小增量）**：KvmFactory.checkHostStatus **原折叠逻辑一行不动**（allMatch→整机行为与历史一致），只在 success 里加 2 行 forEach 把 NodeHealthy 每协议 fire-and-forget 发 `UpdatePrimaryStorageHostStatusMsg(+protocol)`；External handler protocol!=null 时 upsert 新表后直接 reply（不碰 legacy 行）。per-protocol 行=明细可观测层，不改变 HA/调度语义。
- **AddStorageProtocol 触发主机准备**：`PrimaryStorageControllerSvc.onProtocolAdded` default 钩子；ZbsStorageController 实现 Vhost→对 Connected hosts 发 `VHOST_TARGET_ENSURE`，结果 fire-and-forget 写协议行（防 PS 队列死锁）；失败不阻塞 API（ping 自愈）。
- **查询 API**：`APIQueryExternalPrimaryStorageHostProtocolRefMsg`（GET /v1/external-primary-storage/host-protocol-refs，AutoQuery）+ SDK/ApiHelper regen。
- **systag**：`volumeProtocol::{protocol}` 当时只留定义（reserved）不接入（**已于 2026-06-15 接入**，改 `EphemeralPatternSystemTag`，见顶部待办 2）；DiskOffering.protocol 用户砍掉。
- **IT**：ZbsVhostVolumeCase 新增 testAddProtocolPreparesHostsAndRecordsProtocolRefs（删协议行→API 重加→ensure 模拟器断言→协议行→查询 API→ping 驱动 Vhost 行独立翻转→自愈），全 case 过。顺手修了 VHOST_DEACTIVATE 无模拟器的历史 flake。
- **真机（双环境 2026-06-12）**：①两环境各 6 行（3 host×CBD/Vhost）ping 自动建 Connected；②REST 查询 200；③**docker stop zbs-vhost → Vhost 行 Disconnected、CBD 行 Connected**（按协议明细精确定位坏的数据面；host-level 行保持折叠语义照旧判挂——该真机验证早于"最小增量"收敛，收敛后 host-level 行为=历史版）；④删协议行→REST AddProtocol(Vhost)→246.239 容器重建、**248.246 全新自动部署**（pull+容器 26s Up）、242.235 无 docker 如实 Disconnected 且 API 成功。
- **部署教训**：persistence.xml 是构建合并产物（premium 实体），不能整文件覆盖，要 sed 插行；apihelper 产物在 ~/ApiHelper.groovy 需手工回填 testlib。
- 升级 SQL 落在 V5.5.28__schema.sql（原空文件）；env1/env2 已手工建表。

### env2 (172.24.248.246) 干净环境 E2E + 加固代码实测（2026-06-11）✅
加固版（`6c523c4cc`）在第二套环境从零跑通全链路，5 条 review 修复中 3 条拿到 live 证据：
- **部署**：env1 MN 4 jar（header/storage/zbs/sdk，md5 比对一致）+ serviceConfig 2 xml → env2 `/usr/local/zstacktest`（备份 `/root/vhost-rework-backup-env2/`）；properties 加 `Zbs.vhost.targetImage`；DB 插 PS `d402277a` 的 Vhost outputProtocol 行（列名是 `outputProtocol` 不是 protocol）；MN 重启 OK。agent 4 .py（**加固版** target/rpc/storage/vm_plugin）→ host 246.239 py3.11 venv（备份 `/root/vhost-py-backup/`），ansible reconnect 没冲掉 .py（md5 复验）。
- **clean-slate 起点实证**：0 镜像/0 容器/daemon.json 无 insecure-registries/大页 0。
- **E2E 主链**：CBD root（imagestore ttylinux qcow2 `146e9570`，实为 CentOS7）+ 显式 protocol=Vhost 数据卷 `d863c14f` → start 钉 246.239 → **21 秒 Running**。链路内自动完成：**先正常 docker pull→TLS 错→log WARNING "retrying registry as insecure"→写 daemon.json→重试拉下 247MB**（fix #5 条件降级 live 证据）→ 容器起 → 大页 0→335 → vhostuser socket 连。guest（netns ssh root/password）vdb 4Kn → mkfs.xfs/mount/16MB direct/remount md5 OK。
- **reclaim E2E**：ZStack stop → 大页 335→185（容器 reserve 保留）。
- **健康三态 live**（fix #3）：running+sock→True；`docker stop`→**False**（旧代码这里误报 True）；`docker rm -f`→True。
- **幂等重部署**：容器被 rm 后再 start VM → **8 秒** Running，容器重建，日志 pull 总次数仍 1（image_present 短路，零重复拉取），大页回 335。
- 未 live 覆盖：fix #2 fallback（registry 正常时走不到 tar/url，单测覆盖）、fix #4 锁（单流程无并发窗口，单测覆盖）。
- **env2 残留**：VM `vhost-e2e-env2`(282235df, Stopped) + 卷 `vhost-e2e-dv`(d863c14f, Vhost)；rework jar/xml/properties（回滚用 backup-env2）；246.239 上 .py/镜像/容器/daemon.json registry 项。
- **ansible kvmagent tar 已刷加固版（两环境，2026-06-11）**：env1+env2 MN 的 `WEB-INF/classes/ansible/kvm/kvmagent-5.5.0.tar.gz` 注入 6c523c4cc 的 4 .py（内层 md5=eb0b5b28 与本地一致；env1 原 tar 是 6/9 旧版注入、env2 原 stock）。原 tar 备份 `${TAR}.orig-backup`。Reconnect 物理机现在会铺**加固版**而非冲掉。SPDK 镜像无 tar 形态：只在 registry `172.26.208.212:5000`，`/var/lib/zstack/zbs-vhost-image.tar` 两环境都不存在（tar/url 只是 load_image 兜底参数，从未用过）。

### 数据盘 vhost guest 级读写验证（2026-06-11）✅ 闭环
方法：绕开 4Kn-root 不引导 —— **CBD root + Vhost 数据盘**。
- 流程：cold-stop `vhost-livetest5` → detach `vhost-dvtest`(f510f28f, Vhost) → 新建 `cbd-vhost-dvtest`（uuid `e21459f26c434159b17e61ce3f3b83fb`，CreateStopped，root CBD on ZBS PS，钉 190.207）→ attach → start → Running，IP `10.5.166.99`。
- guest 实为 **CentOS 7**（"ttylinux" 镜像 acf309cd = zstack-test-image，有 sshd root/`password`、mkfs.xfs 无 mkfs.ext4）。进 guest 走 host DHCP netns：`ip netns exec br_zsn0_1105_13a0eaea... ssh root@10.5.166.99`，比 send-key 省事得多。
- **Linux 认盘 ✅**：lsblk 见 `vdb` 100M，4Kn（logical/physical block size 都 4096）。**上次 OVMF 不认 root-port 后数据盘 = 纯固件限制，Linux virtio-blk 正常枚举**，疑问解决。
- **裸块 RW ✅**：`dd oflag=direct` 写 8MB → `iflag=direct` 读回，md5 一致。`mount` 报 "write-protected" 是无文件系统时的回退假报警，`/sys/block/vdb/ro`=0。
- **文件系统 RW + 持久 ✅**：mkfs.xfs（sectsz=4096）→ mount（df 确认源=/dev/vdb）→ 32MB direct 写 + proof.txt → sync → umount → remount → md5 OK + 文件完整。
- **SPDK 链路佐证 ✅**：容器内 `rpc.py -s /var/tmp/vhost-sockets/vhost.sock bdev_get_iostat` → `zbs-bdev-b635bfd3...` written 50.7MB / read 75MB，与 guest I/O 量吻合，流量真走 qemu vhost-user → SPDK → ZBS。
- ⚠️ 教训：第一轮 mkfs.ext4 不存在 + mount 失败，dd 全写进 root 盘还 md5 "OK"（假阳性）；**fs 级验证必须 df 确认挂载源**。

### 协议双向切换闭环（2026-06-11）✅ CBD→Vhost 与 Vhost→CBD 都验过
- **A** `cbd2vhost-test`(70afb426)：CLI 默认创建（protocol=CBD）→ `PUT /zstack/v1/volumes/{uuid}/actions {"changeVolumeProtocol":{"protocol":"Vhost"}}` → DB Vhost → attach `cbd-vhost-dvtest` 起机 → host XML **vhostuser** socket `zbs-vhost-a58598eb...` → guest vdc：raw dd direct 写读 md5 一致 + mkfs.xfs/mount/16MB/umount/remount md5 OK。
- **B** `vhost2cbd-test`(11c3f520)：REST 创建显式 protocol=Vhost → changeVolumeProtocol CBD → DB CBD → attach 同 VM → host XML **network CBD** 盘 serial=11c3f520（无 vhost socket）→ guest vdd：raw + fs 持久全过。
- 未挂载卷切协议不受 VM 状态限制（interceptor 停机校验只约束已挂卷），Ready 态直接切成功。
- **guest 识盘方法**：vhost-user-blk 的 serial = SPDK bdev 名（`zbs-bdev-<hash>`，hash 与 per-vol socket 名一致）；CBD 盘 serial = volume uuid 前 20 字符。`cat /sys/block/vd*/serial` 即可精确映射，不用猜设备序。

### 扇区语义 + 带数据跨扇区切换（2026-06-11 续）✅
- 实测确认：CBD = **512e**（guest logical 512 / physical 4096，qemu 块层 RMW 桥），vhost = **4Kn**（4096/4096）。切协议不动字节内容，只改 guest 看到的 logical sector size。
- **带数据双向切换实测过**：A（Vhost/4Kn 下写的 xfs+16MB 数据）切到 CBD/512e → mount + md5 OK；B（CBD/512e 下写的）切到 Vhost/4Kn → mount + md5 OK。前提=整盘 xfs sectsz=4096（mkfs.xfs 在 CBD 上因 physical=4096 hint 默认就选 4096，默认路径安全）。
- **数据盘双向切换的边界**：带 MBR/GPT 分区表的盘（GPT header 在 LBA1，512↔4096 下字节偏移不同）切换后分区表解释错位，**双向都坏**（推理确定，未实测）；显式 `mkfs.xfs -s size=512` 的 fs 切 4Kn 后 mount 拒（fs sectsz < device logical）。
- **根盘双向都不能安全切（已装 OS）**：CBD→Vhost = 512-LBA 装的 GPT/ESP 在 4Kn 下找不到 + SeaBIOS 一律 fail（vhost-livetest5 卡 UEFI shell 实证）；Vhost→CBD = 4Kn 装的 GPT 写在 byte 4096，512e 下 LBA1=byte 512 同样找不到。**不存在"根盘只能 vhost→cbd"的单向通道**。唯一正解 = ZBS 出 512e bdev 消除扇区差（长期方案，已记）。
- 产品防护 follow-up：changeVolumeProtocol interceptor 当前不区分 root/data，根盘切换=自助翻车，考虑拒绝 root volume 或强警告。

### blockio logical 4K 解锁 Vhost→CBD 切换（2026-06-11 实测）✅
用户提出：CBD 侧指定 block io 4K 应该就能切。**实测成立**（数据盘 MBR 级证明，190.207 手工 domain XML）：
- 实验：B 卷在 Vhost/4Kn 下 fdisk 建 MBR（vdd1 起始 LBA 256=byte 1MiB）+ xfs + 16MB 数据 → 切 CBD 默认 512e → vdd1 被解释成 12.4M、起点 128KiB、blkid 无 fs、mount 失败（砖）→ 给该盘 XML 的 blockio 加 `logical_block_size='4096'`（ZStack 原生已发 `physical_block_size='4096'`，**只缺 logical**）→ virsh create → 分区恢复 99M、xfs 直挂、md5 OK。
- 注意：该盘 XML 已有 `<blockio physical_block_size='4096'/>`，**插第二个 blockio 元素会被 libvirt 静默丢弃**，必须改写已有元素。
- 含义：**Vhost→CBD 根盘切换可行**（4Kn 装的 OS 切 CBD 后加 logical 4K，UEFI 引导语义不变）；且 CBD 4K 化后 CBD↔Vhost 双向都安全（两端同为 4Kn）。代价=该卷永失 SeaBIOS legacy 兼容（seabios blksize!=512 fail）。
- 反向不可对称修复：vhost-user-blk 的 block size 来自 SPDK 后端 blkcfg（VHOST_USER_GET_CONFIG），qemu XML 无法 override → 512 装的 OS 切 Vhost 仍无解，只能等 ZBS 512e bdev。
- 产品化落点：changeVolumeProtocol Vhost→CBD 时给卷打标（如 sectorSize=4096），KVM agent 生成 CBD 盘 XML 时按标发 logical 4096。未做，记 follow-up。
- 残留：实验后已 virsh destroy + ZStack 标准重起（B 回到默认 512e，其 MBR 视图重新"砖"，测试卷无害）。boot 级（OVMF 真引导 4Kn CBD 根盘）未直接测，置信高（OVMF 已证能引导 4Kn vhost，virtio config 语义同）。

### 单 vhost（更早的 session，已 done）
- Alpine 3.23 装到 ZBS vhost 4Kn 盘、UEFI+Q35 独立引导到 login（真机）。
- 块大小根因锁定：bdev_zbs 暴露 4Kn → SeaBIOS(legacy) 起不来（seabios `virtio-blk.c:146 blksize!=512 goto fail`，Dell/MS/Intel 文献 + qemu 源码三方印证）；cbd 能 legacy 是 qemu 块层做 512↔4096 RMW（`cbd.c:467 request_alignment=4096` + `io.c` pad），vhost 旁路块层故无此桥。正解 = ZBS 出 512e bdev（长期），当前用 UEFI+4K 镜像。
- Groovy 全链路 case `ZbsVhostVolumeCase` 过；MR !10101(zstack)/!7208(utility) target 5.5.28。

### 本 session 增量（代码全部 compiled + Groovy 过 + pushed）
3 个功能：
1. **全链路自动部署（懒加载）** — `zbs_vhost_target.py`：首次 activate 时 `load_image` 主路径 **registry docker pull**（从 image ref 解析 registry，自动配 insecure-registry + SIGHUP reload + pull），tar/url 兜底；`compute_cores` 按主机 CPU 取高位核；`ensure_target` 懒起容器。
2. **vhost health** — `reportNodeHealthy` 加 Vhost 分支（gate `supportsVhost()`）→ 新 `VHOST_TARGET_HEALTH_PATH` 端点（`target_healthy`：未部署=健康、部署但 sock 死=不健康）。
3. **切换协议 API** — `APIChangeVolumeProtocolMsg`（离线、卷级、interceptor 校验：卷在 external PS、目标协议在 PS outputProtocols、VM 停机），handler 在 `ExternalPrimaryStorage.doChangeVolumeProtocol` 更新 `VolumeVO.protocol`。新 API 进了 `conf/serviceConfig/primaryStorage.xml` + SDK regen（`ChangeVolumeProtocolAction`）。

commit: zstack `757b06acb9`（+base `a9bc5f4d8a`）、utility `92e36805f`，已 push。

### 懒加载全链路真机已验证（env1，2026-06-09，host 190.207）✅ 本次完成
切 `vhost-livetest5` root CBD→Vhost + UEFI/q35 tag，强制起在 **190.207**（242.132 无 docker，跳过）。一次跑通：
- ✅ **registry 自动 pull**：`load_image` 从 image ref 解析 registry → 自动写 `daemon.json` `insecure-registries:[172.26.208.212:5000]` + `systemctl reload docker` + `docker pull`，image `zbs-vhost-x86_64:fc5404056` 落地。
- ✅ **容器自动起**：`zbs-vhost` 容器 running，control sock `/var/tmp/vhost-sockets/vhost.sock` ready。
- ✅ **hugepage 自动分配**：plugin `ensure_hugepages` 配 256 页（compact_memory 后）。
- ✅ **vhost 盘挂载**：domain XML `<disk type='vhostuser'>` target vda boot order 1；SPDK 建 per-vol sock `zbs-vhost-edc5e2d5...`；qemu cmdline `vhost-user-blk-pci ... bootindex=1`；`ss -x` 实证 qemu↔SPDK socket 已连。
- ✅ **VM Running + VNC**：domid 3，VNC `:1`→5901 RFB 003.008 握手通。

#### ⚠️ 本次发现关键 gap：hugepage 只 size 了 target，没算 guest RAM
首次 start 失败：`qemu-kvm: unable to map backing store for guest RAM: Cannot allocate memory`。
根因：vhost-user-blk 强制 guest RAM 走共享 hugepage（qemu `-object memory-backend-memfd hugetlb:true share:true prealloc:true`），即**每个 vhost VM 的 guest 内存也吃 hugepage**（300MB guest = 150×2MB 页）。但 `DEFAULT_HUGEPAGE_NR=256` 只够 SPDK target（~185 页），剩 71 < 150 → 分配失败。
临时绕过：手动 `echo 768 > nr_hugepages`（host 24G）→ 583 free → start 成功。
**待修（代码）**：`ensure_hugepages` 要 size = target_reserve + Σ(本机 vhost VM guest RAM)，或 activate 时按需扩容；否则多/大 vhost VM 必撞墙。这是懒加载落地必修项，优先级高于下面的 framework 增强。

### 真机已验证（env1 172.24.191.9）
- 部署：4 jar(header/sdk/storage/zbs) + serviceConfig + GlobalProperty 到 `/usr/local/zstacktest/`，agent .py 到 3 台主机，MN 重启 OK（无 API 错误）。备份在 `/root/vhost-jar-backup`。
- ✅ **vhost health 端点**：MN 调 `/zbs/primarystorage/vhost/target/health`，3 台主机回 `healthy:true`。
- ✅ **切换协议 API**：REST `POST /zstack/v1/volumes/{uuid}/protocol {"params":{"protocol":"Vhost"}}` 把数据卷 CBD→Vhost，DB 确认（CLI 不认新命令，必须走 REST）。
- ✅ **VM 能起**：CBD VM `vhost-livetest5`(uuid 336967d0, root 3905ed5e) Running。

### 创建时指定 volume protocol（2026-06-09）✅ 真机验证
之前只有 changeVolumeProtocol（事后切，更新路径）。补齐 create 路径：建卷时直接指定协议，覆盖 PS defaultProtocol。
- 改动（5 处）：`APICreateDataVolumeMsg` 加 `@APIParam(required=false) protocol`；`CreateDataVolumeMsg` 加 protocol 字段；`VolumeManagerImpl.handle(APICreateDataVolumeMsg)` 透传 + `handle(CreateDataVolumeMsg)` `vo.setProtocol(msg.getProtocol())`；`VolumeApiInterceptor` 加 `validateVolumeProtocol`（protocol 非空→enum 合法 + 必须带 primaryStorageUuid + protocol ∈ PS outputProtocols，复用 change 路径同款 `PrimaryStorageOutputProtocolRefVO` 校验）。create 走 blank instantiate，`ExternalPrimaryStorage` 只在 protocol 空时填 default → 显式协议存活。
- Groovy：`ZbsVhostVolumeCase.testCreateDataVolumeWithExplicitProtocol`（显式 CBD 覆盖 Vhost 默认 + NBD 未暴露被拒）。
- **真机实证（env1 MN，部署 header+storage jar 重启）**：REST `POST /zstack/v1/volumes/data {protocol:Vhost}` → 卷 protocol=Vhost（覆盖 PS default=CBD）；`protocol:NBD` → 拒 `does not expose output protocol[NBD]`；`protocol:Vhost` 无 PS → 拒 `primaryStorageUuid is required`。env1 PS `ZStone_ZBS_PS` outputProtocols={CBD,Vhost} 已 loaded（`PrimaryStorageOutputProtocolRefVO`）。
- **SDK regen / 构建（隔离 .m2 重做后全绿）**：⚠️ 头一次构建没隔离 .m2，全局 `~/.m2` 被并发 build clobber（`utils-5.5.0.jar` 被覆盖致版本 skew）→ `sdk` profile 编 `premium/test-premium` 报 `FieldUtils/TypeUtils/CollectionDSL cannot be resolved`，**误判成 pre-existing breakage（错的）**。`runMavenProfile` 其实 auto-detect `zstack/.m2/repository`——`mkdir + cp -r .m2-baseline/repository` 后自动隔离。隔离重做：premium build SUCCESS → `sdk` regen SUCCESS（出 `CreateDataVolumeAction.protocol`）→ test 模块 compile SUCCESS（`ZbsVhostVolumeCase.testCreateDataVolumeWithExplicitProtocol` 的 `createDataVolume{protocol=}` DSL 解析通过、class 产出）。详见 memory [[feedback_m2_isolation]]。
- 验证齐备：(1) REST 真机三连过（Vhost 建卷成功 + NBD/无PS 被拒）；(2) 隔离构建 + SDK regen 全绿；(3) **Groovy `ZbsVhostVolumeCase` IT 跑通**（Tests run:1 Failures:0 Errors:0，74.6s；log 实证 explicit-nbd 被 `does not expose output protocol[NBD]` 拒）。SDK 生成只动 `sdk/CreateDataVolumeAction.java`。
- **跑 case 才抓到的坑**：`runMavenProfile sdk` 只改 SDK 源码不重装 jar → `.m2` 里 `sdk-5.5.0.jar` 仍无 protocol 字段 → DSL `createDataVolume{protocol=}` 运行时 `MissingPropertyException`（编译期不报）。修：regen 后 `mvn -pl sdk install`。跑 IT case 用 `scripts/run-zstack-case.sh`（flock + stray-JVM 预检 + timeout kill 子树），见 [[feedback_it_case_serial]]。

### 踩坑（关键）
- VM 起不来根因（日志 `PrimaryStorageMainAllocatorFlow`）：**镜像类型不匹配** —— Ceph-BS 上的镜像 `possiblePrimaryStorageTypes=["Ceph"]`，ZBS PS 是 **Addon** 类型 → 被剔除。**必须用 imagestore BS 上的镜像**（`imagestore1` 69665b28...），如 ttylinux qcow2 `acf309cd17284144a5a7d130767a6ef4`。Ceph-BS 镜像（如 ttylinux raw 5754344f）在 Addon PS 上用不了。
- `ExternalPrimaryStorageHostRefVO.protocol` 列是**死字段**：从不 setProtocol、从不被 query、allocator 也不引用 → 协议级主机连接未实现。
- AddStorageProtocol 不触发 host connect（host ref 靠 `ExternalPrimaryStorageKvmFactory.checkHostStatus`→reportNodeHealthy→UpdatePrimaryStorageHostStatusMsg 按 status 变化才建）。
- `reportNodeHealthy` 消费方 `ExternalPrimaryStorageKvmFactory:240` 用 `allMatch(Ok)` 折叠协议 → 加了 Vhost 后，vhost target 死会把整机对该 PS 判 Disconnected（含 CBD）。隐患，待收敛。
- commit-msg hook 3 警告即停：body 每行 ≤72 消 line-length 警告。worktree `.git` 是文件 → `zdev_git_commit` 直 commit 报 ENOTDIR，用 dry_run 取 Change-Id 再 `git commit -F`。

## 下一步要做

1. ~~**补完懒加载真机验证**~~ ✅ DONE（见上「懒加载全链路真机已验证」）。残留只剩 guest 不引导（ttylinux 512 on 4Kn，预期；要 4K-UEFI 镜像如 Alpine 才进 login）。
1b. ~~**修 hugepage sizing**~~ ✅ DONE（2026-06-09，free-based 按需分配）。
   - 设计：放弃固定 256 / libvirt 汇总 / MN 传字段。用 **free-based 增量**——内核已报 `HugePages_Free`，"running guests + 容器" 隐含在 `total-free` 里。规则：起 VM 那刻 `need=ceil(vm_mem/2MB)`，`free<need` 才长 `(need-free)`（compact 后）。零 MN 改动、零 libvirt 枚举、zbs+expon 通用。
   - 落点：`zbs_vhost_target.py` 加 `mem_to_pages/domain_vhostuser_present/domain_memory_bytes/ensure_free_hugepages/ensure_hugepages_for_domain/reclaim_hugepages`；hook 在 `vm_plugin.Vm.start()` `createWithFlags` 前（gate=domain 有 vhostuser 盘，非 vhost VM no-op，函数内 lazy import 避免 core→zbs 顶层耦合）；`vhost_deactivate` 收尾调 `reclaim_hugepages()`（缩到 `used+slack`，只回收 free 页、不碰容器/在跑 guest）；`ensure_target` 的 256 reserve 改走同一 free-based 路径。
   - 单测：`tests/unit/kvmagent/test_zbs_vhost_hugepage.py` 17/17 过（pages 数学、XML gate/解析、grow/raise/reclaim mock）。
   - **真机实证（env1 190.207）**：停机→deactivate→reclaim 把 768→185（容器 reserve，free 0，容器仍 Up）；起机→start hook 自动 185→335（+150 guest，**无手工 echo**）→VM Running、qemu↔SPDK socket 连、VNC :1。
   - 残留小坑：live-migration 目标机走 `createWithFlags(0)`(line~13048) 不经 `Vm.start()`，未覆盖；vhost VM 迁移本就 offline-only，暂不修。
2. **per-protocol 主机连接（设计决策）**：是否把 `ExternalPrimaryStorageHostRefVO.protocol` 做活 —— host ref 带 protocol + AddStorageProtocol 按协议连主机建 ref + allocator 校验协议。framework 级增强。
3. **收敛 reportNodeHealthy 健康语义**：别让 Vhost 健康拖垮整机 CBD connected（allMatch → per-protocol）。
4. **AddStorageProtocol 触发 host connect**（加协议即可用）。
5. **清理测试残留**：vhost-livetest2-5 VM（多数失败回滚）、vhost-switch-test 数据卷(已 Vhost)、env1 上加的 GlobalProperty/jar（如要还原用 /root/vhost-jar-backup）。
6. **MR review/合并**（!10101 / !7208）。
7. 另 2 套 env 没碰：`172.24.248.246`(2iscsi+zbs+zstone)、`172.24.249.111`(3iscsi+多池zbs+zstone)。

## 环境速查（env1）
- MN+host: `172.24.191.9`；另 2 host: `172.24.190.207`、`172.24.242.132`。SSH root 密码 `password`，admin 账号密码 `password`（**不是** `admin`，zstack-cli LogInByAccount 实测），DB `mysql -uzstack -pzstack.password zstack`。
- ZBS PS: `ZStone_ZBS_PS` uuid `fbfc898799694dfa9336d09690bd7e41`（Addon/zbs），已挂 CBD+Vhost，cluster `855b13ef`。
- registry: `172.26.208.212:5000/zbs-vhost-x86_64:fc5404056`（tags eb6763cd5/fa28a8261/fc5404056），主机可达。GlobalProperty `Zbs.vhost.targetImage` 已设此值。
- Addon-可用镜像: imagestore1 上的 ttylinux qcow2 `acf309cd17284144a5a7d130767a6ef4`。L3 `13a0eaea`，offering small-vm `7e53a68e`。
- kvmagent plugins: `/var/lib/zstack/virtualenv/kvm/lib/python3.11/site-packages/kvmagent/plugins/`；重启 `service zstack-kvmagent restart`。MN 包 `.../ansible/kvm/kvmagent-5.5.0.tar.gz`（已注入 vhost .py）。
- worktree: `/home/mj/zstack-workspace/worktrees/feature-zbs-vhost/`（zstack + premium + zstack-utility，branch feature-zbs-vhost）。
