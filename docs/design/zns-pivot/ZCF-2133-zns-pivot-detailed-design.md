# ZCF-2133 ZNS 集成改造 P3 详细设计文档

## 1. 文档信息

| 字段 | 内容 |
|------|------|
| 文档编号 | ZCF-2133-P3-DD |
| 版本 | 1.1.0 |
| 状态 | 草稿 |
| 所属阶段 | P3 详细设计（瀑布模型第三阶段） |
| 关联 Func Spec | ZCF-2133 Func Spec v2.2.2 |
| 编写日期 | 2025-07 |
| 最后更新 | 2025-07 |

### 修改历史

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.1.0 | 2026-04 | — | Review 修订：P0-1 HTTP 机制改 Spring @Controller；P0-2 补全 evtf/znsApiClient 注入；P0-3 AddComputeManager 增加事务边界+幂等语义；P0-4 补充 VmNicVO.type 赋值时机说明；P1-1/P1-3 Handler 错误响应修正；P1-2 并发序列化保护；P1-4 幂等检查改 computerManagerUuid；P2-2 cleanupOrphanSegments 增加 CM 过滤；P2-3 scope 值抽象化 |
| 1.0.0 | 2025-07 | — | 初稿，覆盖 14 个设计点 |

---

## 2. 设计目标与范围

### 2.1 目标

将 ZNS 插件从"Cloud 主动拉取/注册"模式改造为"ZNS 主动推送通知"模式（pivot），实现：

1. **反向通知通道**：ZNS → Cloud 的 HTTP 推送，替代现有纯拉取模型
2. **Add-CM 流程重写**：ZNS wizard 完成后主动调用 Cloud，Cloud 被动建档
3. **初次同步（Wizard Init Sync）**：ZNS wizard 完成 T1 创建后触发全量拉取
4. **租户资源建模**：新增 `ZnsTenantVO` / `ZnsTenantRouterVO` 及完整生命周期
5. **SystemTag 迁移**：`enableDpdkVhostuser`（L3 维度）→ `znsNicMode`（VM 维度）
6. **VPC L3 支持**：ZnsApiInterceptor 放开对 `L3VpcNetwork` 的拦截，要求关联租户路由器 Tag
7. **DPDK NIC 生命周期**：最小化路径实现 DPDK vhostuser socket 的创建/释放
8. **Reconcile SoT 拆分**：设备资源（ZNS-as-SoT）与网络资源（Cloud-as-SoT）分组

### 2.2 范围

本文档覆盖 ZNS 插件目录：
```
zstack/premium/plugin-premium/zns/src/main/java/org/zstack/network/zns/
```
以及关联的 KVM 插件改动（DPDK NIC 命令）和 VM NIC 类型计算改动。

### 2.3 不在范围内

- ZNS 侧 API 实现（由 ZNS 团队负责）
- KVM agent（Python）侧 dpdk socket 实现
- UI/前端改动
- 性能测试与压测

---

## 3. 设计变更概览

| 编号 | 变更点 | 变更类型 | 涉及类 |
|------|--------|----------|--------|
| 3.1 | 新增反向通知通道 | 新增 | `ZnsNotificationController`, `ZnsNotificationCommands` |
| 3.2 | Add-CM 流程重写 | 修改+新增 | `ZnsNotificationController.AddComputeManagerHandler`, `ZnsSdnController` |
| 3.3 | 移除 Flow 5 `create-l2-l3-from-segments` | **删除** | `ZnsSdnController.initSdnController` |
| 3.4 | 首次同步（Wizard Init Sync） | 新增 | `ZnsNotificationController.WizardInitSyncHandler` |
| 3.5 | 租户资源建模 | 新增 | `ZnsTenantVO`, `ZnsTenantRouterVO`, `ZnsTenantInventory`, `ZnsTenantRouterInventory` |
| 3.6 | SystemTag 变更 | 修改 | `ZnsSdnControllerSystemTags` |
| 3.7 | ZnsApiInterceptor VPC 支持 | 修改 | `ZnsApiInterceptor` |
| 3.8 | VM NIC 类型计算 | 修改 | `VmNicManagerImpl.computeVmNicType` |
| 3.9 | DPDK NIC 生命周期 | 新增 | `KVMRealizeL2GeneveNetworkBackend`, `KVMAgentCommands` |
| 3.10 | Reconcile SoT 拆分 | 重构 | `ZnsSdnController.reconnectSdnController` |
| 3.11 | 运行时推送通知处理 | 新增 | `SyncFabricHandler`, `SyncResourceHandler` |
| 3.12 | DHCP/DNS/MTU 完整性 | 验证+小修 | `ZnsSdnControllerDhcp` |
| 3.13 | 双栈 IpRange | 验证 | `ZnsSdnControllerL3` |
| 3.14 | 新增错误码 | 新增 | `ZnsErrors` |

---

## 4. 详细设计

### 4.1 反向通知通道

#### 4.1.1 设计说明

当前 ZNS 插件只有 `/zns/callback` 处理异步任务回调。新增业务事件推送通道，支持 ZNS → Cloud 的主动通知，实现推送模型替代拉取模型。

**HTTP 机制选型**：ZNS 是外部系统，HTTP 请求不携带 ZStack 内部 `commandpath` 路由头，因此**不能**使用 `RESTFacade.registerSyncHttpCallHandler`（该机制仅适用于 ZStack 内部 Agent 协议）。改用 Spring `@Controller` + `@RequestMapping`，与现有 `ZnsCallbackController` 保持一致。

所有端点不做鉴权（Phase 1 依赖网络隔离），预留鉴权钩子供后续版本升级（Phase 2 计划 HMAC 签名验证）。

#### 4.1.2 新增类：`ZnsNotificationController`

**包路径**：`org.zstack.network.zns`

```java
@Controller
@Component
public class ZnsNotificationController {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;                 // P0-2: 必需，AddComputeManagerHandler 触发规范事件
    @Autowired
    private ZnsApiClient znsApiClient;        // P0-2: 必需，SyncResourceHandler 拉取资源详情
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    @RequestMapping(
        value   = ZnsConstant.ZNS_NOTIFY_ADD_COMPUTE_MANAGER_PATH,
        method  = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handleAddComputeManager(
            @RequestBody ZnsNotificationCommands.AddComputeManagerNotification cmd) {
        return new AddComputeManagerHandler().handle(cmd);
    }

    @RequestMapping(
        value   = ZnsConstant.ZNS_NOTIFY_WIZARD_INIT_SYNC_PATH,
        method  = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handleWizardInitSync(
            @RequestBody ZnsNotificationCommands.WizardInitSyncNotification cmd) {
        return new WizardInitSyncHandler().handle(cmd);
    }

    @RequestMapping(
        value   = ZnsConstant.ZNS_NOTIFY_SYNC_FABRIC_PATH,
        method  = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handleSyncFabric(
            @RequestBody ZnsNotificationCommands.SyncFabricNotification cmd) {
        return new SyncFabricHandler().handle(cmd);
    }

    @RequestMapping(
        value   = ZnsConstant.ZNS_NOTIFY_SYNC_RESOURCE_PATH,
        method  = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handleSyncResource(
            @RequestBody ZnsNotificationCommands.SyncResourceNotification cmd) {
        return new SyncResourceHandler().handle(cmd);
    }

    /** 鉴权预留钩子，Phase 1 直接返回，Phase 2 验证 HMAC 签名 */
    private void validateRequest(HttpServletRequest req, String controllerUuid) {
        // reserved: no-op in phase 1
    }

    /** 根据 computerManagerUuid 系统标签查找 SdnControllerVO */
    private SdnControllerVO findControllerByComputerManagerUuid(String computerManagerUuid) {
        String tagPattern = ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID
            .instantiateTag(map(e(
                ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID_TOKEN, computerManagerUuid
            )));
        List<String> controllerUuids = Q.New(SystemTagVO.class)
            .select(SystemTagVO_.resourceUuid)
            .eq(SystemTagVO_.tag, tagPattern)
            .eq(SystemTagVO_.resourceType, SdnControllerVO.class.getSimpleName())
            .listValues();
        if (controllerUuids.isEmpty()) {
            return null;
        }
        return dbf.findByUuid(controllerUuids.get(0), SdnControllerVO.class);
    }

    /** 在事务内完成 SdnControllerVO + ZnsControllerVO + SystemTag 的原子创建，返回 controllerUuid */
    @Transactional
    private String persistNewController(ZnsNotificationCommands.AddComputeManagerNotification cmd) {
        SdnControllerVO sdnVo = new SdnControllerVO();
        sdnVo.setUuid(Platform.getUuid());
        sdnVo.setName(cmd.name);
        sdnVo.setDescription(cmd.description);
        sdnVo.setIp(cmd.vip);
        sdnVo.setUsername(cmd.account);
        sdnVo.setPassword(cmd.password);
        sdnVo.setVendorType(ZnsSdnControllerFactory.ZNS_TYPE);
        sdnVo.setStatus(SdnControllerStatus.Connected);
        dbf.getEntityManager().persist(sdnVo);

        ZnsControllerVO znsVo = new ZnsControllerVO();
        znsVo.setUuid(sdnVo.getUuid());
        dbf.getEntityManager().persist(znsVo);

        ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID.createInherentTag(
            sdnVo.getUuid(),
            map(e(ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID_TOKEN, cmd.computerManagerUuid))
        );

        return sdnVo.getUuid();
    }

    // 内部 Handler 类见 §4.2 ~ §4.4 / §4.10
    class AddComputeManagerHandler { ... }
    class WizardInitSyncHandler { ... }
    class SyncFabricHandler { ... }
    class SyncResourceHandler { ... }
}
```

**新增常量**（`ZnsConstant.java`）：

```java
public static final String ZNS_NOTIFY_ADD_COMPUTE_MANAGER_PATH = "/zns/notify/add-compute-manager";
public static final String ZNS_NOTIFY_WIZARD_INIT_SYNC_PATH     = "/zns/notify/wizard-init-sync";
public static final String ZNS_NOTIFY_SYNC_FABRIC_PATH          = "/zns/notify/sync-fabric";
public static final String ZNS_NOTIFY_SYNC_RESOURCE_PATH        = "/zns/notify/sync-resource";
```

#### 4.1.3 新增类：`ZnsNotificationCommands`

**包路径**：`org.zstack.network.zns`

```java
public class ZnsNotificationCommands {

    /** ZNS wizard 完成 addComputeManager 后推送给 Cloud */
    public static class AddComputeManagerNotification {
        /** ZNS VIP，后续 API 调用使用 */
        public String vip;
        /** ZNS 管理员账号 */
        public String account;
        /** ZNS 管理员密码 */
        public String password;
        /** ZNS 侧 computerManager UUID */
        public String computerManagerUuid;
        /** 人类可读名称 */
        public String name;
        public String description;
    }

    /** ZNS wizard 创建第一个 T1（租户路由器）后推送给 Cloud */
    public static class WizardInitSyncNotification {
        public String computerManagerUuid;
    }

    /** 传输区域/节点/Host-Switch 配置变更时推送 */
    public static class SyncFabricNotification {
        public String computerManagerUuid;
        /**
         * 同步范围：ALL | FABRIC | TENANT，默认 ALL
         * ALL    - 全量同步设备资源（TZ+TN+Tenant+TenantRouter）
         * FABRIC - 仅同步传输层资源（TransportZone + TransportNode + HostSwitch）
         * TENANT - 仅同步租户资源（Tenant + TenantRouter）
         *
         * 注：不直接暴露 ZNS 内部术语（TZ/TN/HOSTSWITCH），使用 Cloud 视角抽象值。
         */
        public String scope;
    }

    /** Segment 或 TenantRouter 创建/更新/删除时推送 */
    public static class SyncResourceNotification {
        public String computerManagerUuid;
        /** SEGMENT | TENANT_ROUTER */
        public String resourceType;
        /** CREATE | UPDATE | DELETE */
        public String action;
        /** ZNS 侧资源 UUID（dashed 格式） */
        public String resourceUuid;
    }

    /** 通用响应 */
    public static class NotificationResponse {
        public boolean success;
        public String uuid;
        public String error;
    }
}
```

---

### 4.2 Add-CM 流程重写

#### 4.2.1 设计说明

**旧流程**（主动模式）：Cloud 调用 `preInitSdnController` → 向 ZNS 发起 `verifyComputeManager` → 拉取集群信息 → 建档。

**新流程**（被动模式）：ZNS wizard 完成后调用 `POST /zns/notify/add-compute-manager` → Cloud 接收通知 → 直接建档 → 返回 controller UUID 给 ZNS。

**向后兼容**：保留现有 `APIAddSdnControllerMsg` + `preInitSdnController` 作为手动补录通道，在常量中记录废弃说明。

#### 4.2.2 `AddComputeManagerHandler.handle`

```java
class AddComputeManagerHandler {

    public String handle(ZnsNotificationCommands.AddComputeManagerNotification cmd) {

        // 1. 幂等检查：同 computerManagerUuid 已存在则直接返回现有 UUID（幂等语义，而非报错）
        //    真正的业务唯一键是 computerManagerUuid，name 可能变化，不作唯一性依据。
        SdnControllerVO existing = findControllerByComputerManagerUuid(cmd.computerManagerUuid);
        if (existing != null) {
            return JSONObjectUtil.toJsonString(result(true, existing.getUuid(), null));
        }

        // 2-4. 在同一事务内原子创建 SdnControllerVO + ZnsControllerVO + SystemTag
        //      避免部分写入导致的孤岛记录。
        String controllerUuid = persistNewController(cmd);

        // 5. 发送规范事件 SDN_CONTROLLER_ADDED（evtf 已在外部类注入）
        SdnControllerVO created = dbf.findByUuid(controllerUuid, SdnControllerVO.class);
        SdnControllerCanonicalEvents.SdnControllerAddedData evtData =
            new SdnControllerCanonicalEvents.SdnControllerAddedData();
        evtData.setControllerUuid(controllerUuid);
        evtData.setInventory(SdnControllerInventory.valueOf(created));
        evtf.fire(SdnControllerCanonicalEvents.SDN_CONTROLLER_ADDED, evtData);

        // 注：password 字段在日志打印时必须脱敏，不得原文输出到 debug log。
        logger.debug(String.format("[add-compute-manager] SDN controller[uuid:%s] created " +
            "for computerManagerUuid[%s]", controllerUuid, cmd.computerManagerUuid));

        // 6. 返回 controller UUID
        return JSONObjectUtil.toJsonString(result(true, controllerUuid, null));
    }

    private ZnsNotificationCommands.NotificationResponse result(
            boolean success, String uuid, String error) {
        ZnsNotificationCommands.NotificationResponse r =
            new ZnsNotificationCommands.NotificationResponse();
        r.success = success;
        r.uuid    = uuid;
        r.error   = error;
        return r;
    }
}
```

> **事务边界**：步骤 2-4 由外部类的 `persistNewController(@Transactional)` 方法完成，三步 persist 在同一事务内。若任意一步失败则全部回滚，无孤岛记录残留。

#### 4.2.3 `initSdnController` 清理（移除 Flow 5）

在 `ZnsSdnController.initSdnController` 中，删除 Flow 5 `create-l2-l3-from-segments`。

**变更前**（5 个 Flow）：
```
Flow 1: sync-compute-collections
Flow 2: fetch-discovered-nodes
Flow 3: derive-vswitch-and-create-host-refs
Flow 4: persist-transport-zones
Flow 5: create-l2-l3-from-segments   ← 删除此 Flow
```

**变更后**（4 个 Flow）：
```
Flow 1: sync-compute-collections
Flow 2: fetch-discovered-nodes
Flow 3: derive-vswitch-and-create-host-refs
Flow 4: persist-transport-zones
```

`initSdnController` 职责明确为**设备同步**，不再做 L2/L3 反向导入。

**向后兼容常量**（`ZnsSdnControllerConstant.java`）：

```java
/**
 * 手动添加 SdnController 的 API 路径已废弃，保留以支持遗留脚本。
 * 新流程：ZNS wizard 完成后调用 POST /zns/notify/add-compute-manager 自动建档。
 * 将在 ZStack v5.1 移除手动路径。
 */
@Deprecated
public static final String MANUAL_ADD_DEPRECATION_NOTE =
    "APIAddSdnControllerMsg for ZNS is deprecated since v5.0; " +
    "use ZNS wizard push-notification flow instead.";
```

---

### 4.3 首次同步（Wizard Init Sync）

#### 4.3.1 设计说明

ZNS wizard 创建第一个 T1（租户路由器）后，调用 `POST /zns/notify/wizard-init-sync`。Cloud 触发全量 ZNS 数据拉取，同时对"Cloud 无对应 L2 的 ZNS 孤儿 Segment"执行强制删除（Cloud-as-SoT 语义）。

#### 4.3.2 `WizardInitSyncHandler.handle`

```java
class WizardInitSyncHandler {

    public String handle(ZnsNotificationCommands.WizardInitSyncNotification cmd) {

        SdnControllerVO controller = findControllerByComputerManagerUuid(cmd.computerManagerUuid);
        if (controller == null) {
            // P1-1: 返回结构化错误响应，不抛异常（ZNS 需能解析响应体）
            ZnsNotificationCommands.NotificationResponse resp =
                new ZnsNotificationCommands.NotificationResponse();
            resp.success = false;
            resp.error   = String.format(
                "no SDN controller found for computerManagerUuid[%s]",
                cmd.computerManagerUuid);
            return JSONObjectUtil.toJsonString(resp);
        }

        String controllerUuid = controller.getUuid();
        String znsIp          = controller.getIp();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("zns-wizard-init-sync-%s", controllerUuid));
        chain.allowEmptyFlow();

        // Flow 1: 从 ZNS 同步 TransportZone（ZNS-as-SoT）
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-transport-zones-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTransportZonesFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });

        // Flow 2: 从 ZNS 同步 TransportNode / Host Refs（ZNS-as-SoT）
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-transport-nodes-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTransportNodesFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });

        // Flow 3: 从 ZNS 同步 Tenant（ZNS-as-SoT，若 ZNS 暂无租户 API 则 skip）
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-tenants-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTenantsFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) {
                        // 租户 API 缺失时降级跳过，记录 warning 日志
                        logger.warn(String.format(
                            "[zns-wizard-init-sync] tenant API not available " +
                            "for controller[uuid:%s], skipping", controllerUuid));
                        trigger.next();
                    }
                });
            }
        });

        // Flow 4: 从 ZNS 同步 TenantRouter（ZNS-as-SoT）
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-tenant-routers-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTenantRoutersFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });

        // Flow 5: 清理孤儿 Segment（Cloud-as-SoT）
        chain.then(new NoRollbackFlow() {
            String __name__ = "cleanup-orphan-segments";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                cleanupOrphanSegments(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) {
                        // 孤儿清理失败不阻断主流程，记录 warning
                        logger.warn(String.format(
                            "[zns-wizard-init-sync] orphan segment cleanup failed " +
                            "for controller[uuid:%s]: %s", controllerUuid, err.getDetails()));
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(null) {
            @Override public void handle(Map data) {
                logger.info(String.format(
                    "[zns-wizard-init-sync] completed for controller[uuid:%s]", controllerUuid));
            }
        }).error(new FlowErrorHandler(null) {
            @Override public void handle(ErrorCode err, Map data) {
                logger.error(String.format(
                    "[zns-wizard-init-sync] failed for controller[uuid:%s]: %s",
                    controllerUuid, err.getDetails()));
            }
        }).start();

        // 202 Accepted 语义：FlowChain 异步执行，立即返回"已受理"。
        // ZNS 不应假设 Cloud 同步已完成；若需确认同步结果，可在后续操作前
        // 查询 Cloud 的 TenantRouter 列表，或等待 reconnectSdnController 周期补偿。
        ZnsNotificationCommands.NotificationResponse resp =
            new ZnsNotificationCommands.NotificationResponse();
        resp.success = true;
        return JSONObjectUtil.toJsonString(resp);
    }
}
```

#### 4.3.3 孤儿 Segment 清理逻辑

```java
private void cleanupOrphanSegments(String znsIp, String controllerUuid,
                                   Completion completion) {
    // 1. 从 ZNS 拉取该 computerManager 下的所有 Segment 列表
    //    必须携带 computerManagerUuid 过滤参数，避免拉取其他 CM 下的 Segment 导致误删。
    String computerManagerUuid = ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID
        .getTokenByResourceUuid(controllerUuid,
            ZnsSdnControllerSystemTags.COMPUTER_MANAGER_UUID_TOKEN);
    znsApiClient.listSegments(znsIp, computerManagerUuid,
        new ReturnValueCompletion<List<ZnsApiCommands.SegmentData>>(completion) {
        @Override
        public void success(List<ZnsApiCommands.SegmentData> znsSegments) {
            // 2. 对每个 ZNS Segment：检查 Cloud 是否有对应的 L2
            //    判据：systemTag znsSegmentUuid::{segmentUuid} 存在于 L2NetworkVO
            List<String> orphanExternalUuids = new ArrayList<>();
            for (ZnsApiCommands.SegmentData seg : znsSegments) {
                String tagValue = ZnsSdnControllerSystemTags.ZNS_SEGMENT_UUID
                    .instantiateTag(map(e(
                        ZnsSdnControllerSystemTags.ZNS_SEGMENT_UUID_TOKEN, seg.uuid
                    )));
                boolean cloudHasL2 = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.tag, tagValue)
                    .eq(SystemTagVO_.resourceType, L2NetworkVO.class.getSimpleName())
                    .isExists();
                if (!cloudHasL2) {
                    orphanExternalUuids.add(seg.uuid);
                }
            }

            // 3. 批量删除孤儿 Segment（force=true 忽略 ZNS 侧依赖）
            new While<>(orphanExternalUuids).each((segUuid, whileCompletion) -> {
                znsApiClient.deleteSegment(znsIp, segUuid, true,
                    new Completion(whileCompletion) {
                        @Override public void success() { whileCompletion.done(); }
                        @Override public void fail(ErrorCode err) {
                            logger.warn(String.format(
                                "[cleanup-orphan-segments] failed to delete ZNS segment[uuid:%s]: %s",
                                segUuid, err.getDetails()));
                            whileCompletion.done(); // 单条失败不阻断
                        }
                    });
            }).run(new NoErrorCompletion(completion) {
                @Override public void done() { completion.success(); }
            });
        }

        @Override
        public void fail(ErrorCode err) { completion.fail(err); }
    });
}
```

---

### 4.4 租户资源建模

#### 4.4.1 `ZnsTenantVO`

**包路径**：`org.zstack.network.zns`

```java
@Entity
@Table(name = "ZnsTenantVO")
public class ZnsTenantVO extends ResourceVO {

    @Column(nullable = false)
    private String sdnControllerUuid;

    @Column(nullable = false, length = 64)
    private String externalUuid;  // ZNS 侧 tenant UUID（dashed 格式）

    @Column(nullable = false)
    private String name;

    @Column(length = 2048)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Timestamp createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Timestamp lastOpDate;

    // getter/setter 省略（使用 Lombok @Data 或手写均可）
}
```

**Flyway DDL**（文件名：`V5.0.x__ZnsTenantVO.sql`）：

```sql
CREATE TABLE IF NOT EXISTS `ZnsTenantVO` (
  `uuid`               CHAR(32)      NOT NULL,
  `sdnControllerUuid`  CHAR(32)      NOT NULL,
  `externalUuid`       VARCHAR(64)   NOT NULL COMMENT 'ZNS tenant UUID (dashed format)',
  `name`               VARCHAR(255)  NOT NULL,
  `description`        VARCHAR(2048) DEFAULT NULL,
  `createDate`         DATETIME      NOT NULL,
  `lastOpDate`         DATETIME      NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uidx_externalUuid` (`externalUuid`),
  KEY       `idx_sdnControllerUuid` (`sdnControllerUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Inventory 类**：`ZnsTenantInventory`

```java
@InventoryDoc(
    spec = @InventoryDocSpec(
        inventory = "ZnsTenantInventory",
        tableNames = {"ZnsTenantVO"}
    )
)
public class ZnsTenantInventory {
    public String uuid;
    public String sdnControllerUuid;
    public String externalUuid;
    public String name;
    public String description;
    public Timestamp createDate;
    public Timestamp lastOpDate;

    public static ZnsTenantInventory valueOf(ZnsTenantVO vo) {
        ZnsTenantInventory inv = new ZnsTenantInventory();
        inv.uuid               = vo.getUuid();
        inv.sdnControllerUuid  = vo.getSdnControllerUuid();
        inv.externalUuid       = vo.getExternalUuid();
        inv.name               = vo.getName();
        inv.description        = vo.getDescription();
        inv.createDate         = vo.getCreateDate();
        inv.lastOpDate         = vo.getLastOpDate();
        return inv;
    }
}
```

**ZNS API 响应 DTO**（添加到 `ZnsApiCommands.java`）：

```java
public static class TenantData {
    public String uuid;        // ZNS dashed UUID
    public String name;
    public String description;
}
```

#### 4.4.2 `ZnsTenantRouterVO`

```java
@Entity
@Table(name = "ZnsTenantRouterVO")
public class ZnsTenantRouterVO extends ResourceVO {

    @Column(nullable = false)
    private String sdnControllerUuid;

    @Column(nullable = false, length = 64)
    private String externalUuid;  // ZNS 侧 tenant router UUID（dashed 格式）

    @Column
    private String tenantUuid;    // 可选 FK → ZnsTenantVO.uuid

    @Column(nullable = false)
    private String name;

    @Column(length = 2048)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Timestamp createDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Timestamp lastOpDate;
}
```

**Flyway DDL**（文件名：`V5.0.x__ZnsTenantRouterVO.sql`）：

```sql
CREATE TABLE IF NOT EXISTS `ZnsTenantRouterVO` (
  `uuid`               CHAR(32)      NOT NULL,
  `sdnControllerUuid`  CHAR(32)      NOT NULL,
  `externalUuid`       VARCHAR(64)   NOT NULL COMMENT 'ZNS tenant router UUID (dashed format)',
  `tenantUuid`         CHAR(32)      DEFAULT NULL COMMENT 'FK to ZnsTenantVO, nullable',
  `name`               VARCHAR(255)  NOT NULL,
  `description`        VARCHAR(2048) DEFAULT NULL,
  `createDate`         DATETIME      NOT NULL,
  `lastOpDate`         DATETIME      NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uidx_externalUuid` (`externalUuid`),
  KEY       `idx_sdnControllerUuid` (`sdnControllerUuid`),
  KEY       `idx_tenantUuid`        (`tenantUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Inventory 类**：`ZnsTenantRouterInventory`

```java
@InventoryDoc(
    spec = @InventoryDocSpec(
        inventory = "ZnsTenantRouterInventory",
        tableNames = {"ZnsTenantRouterVO"}
    )
)
public class ZnsTenantRouterInventory {
    public String uuid;
    public String sdnControllerUuid;
    public String externalUuid;
    public String tenantUuid;
    public String name;
    public String description;
    public Timestamp createDate;
    public Timestamp lastOpDate;

    public static ZnsTenantRouterInventory valueOf(ZnsTenantRouterVO vo) {
        ZnsTenantRouterInventory inv = new ZnsTenantRouterInventory();
        inv.uuid              = vo.getUuid();
        inv.sdnControllerUuid = vo.getSdnControllerUuid();
        inv.externalUuid      = vo.getExternalUuid();
        inv.tenantUuid        = vo.getTenantUuid();
        inv.name              = vo.getName();
        inv.description       = vo.getDescription();
        inv.createDate        = vo.getCreateDate();
        inv.lastOpDate        = vo.getLastOpDate();
        return inv;
    }
}
```

**Query API**：

```java
// APIQueryZnsTenantRouterMsg.java
@Action(category = SdnControllerConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryZnsTenantRouterMsg
    extends APIQueryMessage implements APISyncCallMessage {

    public static List<String> __example__() {
        return list("uuid=" + uuid());
    }
}

// APIQueryZnsTenantRouterReply.java
public class APIQueryZnsTenantRouterReply
    extends APIQueryReply {
    public List<ZnsTenantRouterInventory> inventories;
}
```

**ZNS API 响应 DTO**（添加到 `ZnsApiCommands.java`）：

```java
public static class TenantRouterData {
    public String uuid;        // ZNS dashed UUID
    public String name;
    public String tenant_uuid; // ZNS 侧 tenant UUID（dashed 格式）
    public String description;
}
```

**ZNS API Client 新增方法**（`ZnsApiClient.java`）：

```java
/**
 * 列出指定 ZNS 控制器下所有 TenantRouter
 */
void listTenantRouters(
    String znsIp,
    ReturnValueCompletion<ListResponse<ZnsApiCommands.TenantRouterData>> completion
);

/**
 * 获取单个 TenantRouter 详情
 */
void getTenantRouter(
    String znsIp,
    String externalUuid,
    ReturnValueCompletion<GetResponse<ZnsApiCommands.TenantRouterData>> completion
);
```

#### 4.4.3 限制删除租户路由器

在 `ZnsApiInterceptor` 中拦截 `APIDeleteZnsTenantRouterMsg`：

```java
private void validate(APIDeleteZnsTenantRouterMsg msg) {
    // 检查是否有 L3 通过 systemTag 引用此 TenantRouter
    String tagPattern = ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID
        .instantiateTag(map(e(
            ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID_TOKEN, msg.getUuid()
        )));
    List<String> l3Uuids = Q.New(SystemTagVO.class)
        .select(SystemTagVO_.resourceUuid)
        .eq(SystemTagVO_.tag, tagPattern)
        .eq(SystemTagVO_.resourceType, L3NetworkVO.class.getSimpleName())
        .listValues();
    if (!l3Uuids.isEmpty()) {
        throw new ApiMessageInterceptionException(
            ZnsErrors.operr(ZnsErrors.ZNS_TENANT_ROUTER_IN_USE,
                "cannot delete tenant router[uuid:%s], L3 networks [%s] are still using it",
                msg.getUuid(), String.join(", ", l3Uuids))
        );
    }
}
```

---

### 4.5 SystemTag 变更

#### 4.5.1 设计说明

`ENABLE_DPDK_VHOSTUSER` 是 L3 维度的 SystemTag，与业务语义不符（dpdk 是 VM 级别的 NIC 模式选择，而非网络属性）。新 Tag `znsNicMode` 改为 VM 维度，值为 `dpdk` 或 `kernel`。

此 Tag 是**新特性 Tag**，无历史数据，可直接重命名。保留旧 Tag 一个版本作为 fallback 读取。

#### 4.5.2 `ZnsSdnControllerSystemTags` 变更

```java
public class ZnsSdnControllerSystemTags {

    // ===== 已有 Tag（保持不变）=====

    /** computerManagerUuid 关联标签，挂在 SdnControllerVO 上 */
    public static final String COMPUTER_MANAGER_UUID_TOKEN = "computerManagerUuid";
    public static final PatternedSystemTag COMPUTER_MANAGER_UUID = new PatternedSystemTag(
        String.format("computerManagerUuid::{%s}", COMPUTER_MANAGER_UUID_TOKEN),
        SdnControllerVO.class
    );

    /** ZNS Segment UUID，挂在 L2NetworkVO 上 */
    public static final String ZNS_SEGMENT_UUID_TOKEN = "znsSegmentUuid";
    public static final PatternedSystemTag ZNS_SEGMENT_UUID = new PatternedSystemTag(
        String.format("znsSegmentUuid::{%s}", ZNS_SEGMENT_UUID_TOKEN),
        L2NetworkVO.class
    );

    // ===== 废弃（Deprecated）=====

    /**
     * @deprecated 自 v5.0 起废弃，改为 VM 维度的 {@link #ZNS_NIC_MODE}。
     *             将在 v5.1 删除。仅作为读取 fallback，不再写入。
     */
    @Deprecated
    public static final String ENABLE_DPDK_VHOSTUSER_TOKEN = "enableDpdkVhostuser";
    @Deprecated
    public static final PatternedSystemTag ENABLE_DPDK_VHOSTUSER = new PatternedSystemTag(
        String.format("enableDpdkVhostuser::{%s}", ENABLE_DPDK_VHOSTUSER_TOKEN),
        L3NetworkVO.class
    );

    // ===== 新增 =====

    /**
     * VM 级别 NIC 模式标签，挂在 VmInstanceVO 上。
     * 取值：dpdk | kernel（缺省值为 kernel）
     */
    public static final String ZNS_NIC_MODE_TOKEN = "mode";
    public static final PatternedSystemTag ZNS_NIC_MODE = new PatternedSystemTag(
        String.format("znsNicMode::{%s}", ZNS_NIC_MODE_TOKEN),
        VmInstanceVO.class
    );

    /**
     * L3 VPC 网络关联的 TenantRouter UUID，挂在 L3NetworkVO 上。
     * 创建 VPC L3 时必须携带此 Tag。
     */
    public static final String ZNS_TENANT_ROUTER_UUID_TOKEN = "tenantRouterUuid";
    public static final PatternedSystemTag ZNS_TENANT_ROUTER_UUID = new PatternedSystemTag(
        String.format("znsTenantRouterUuid::{%s}", ZNS_TENANT_ROUTER_UUID_TOKEN),
        L3NetworkVO.class
    );
}
```

#### 4.5.3 NIC 模式读取兼容逻辑

```java
/**
 * 读取 VM 的 ZNS NIC 模式，优先读新 Tag，fallback 读旧 L3 Tag。
 * @return "dpdk" | "kernel"
 */
public static String resolveZnsNicMode(String vmUuid, String l3Uuid) {
    // 1. 优先读 VM 维度新 Tag
    String newTag = ZnsSdnControllerSystemTags.ZNS_NIC_MODE
        .getTokenByResourceUuid(vmUuid, ZnsSdnControllerSystemTags.ZNS_NIC_MODE_TOKEN);
    if (newTag != null) {
        return newTag;
    }

    // 2. Fallback: 读旧 L3 维度 Tag（deprecated，v5.1 删除此分支）
    String oldTag = ZnsSdnControllerSystemTags.ENABLE_DPDK_VHOSTUSER
        .getTokenByResourceUuid(l3Uuid,
            ZnsSdnControllerSystemTags.ENABLE_DPDK_VHOSTUSER_TOKEN);
    if ("true".equalsIgnoreCase(oldTag)) {
        return "dpdk";
    }

    // 3. 默认 kernel 模式
    return "kernel";
}
```

---

### 4.6 ZnsApiInterceptor 更新

#### 4.6.1 VPC L3 支持

**文件**：`ZnsApiInterceptor.java`

```java
private void validate(APICreateL3NetworkMsg msg) {
    // 前置：获取 L2 上的 ZNS 控制器，非 ZNS L2 则直接放行
    if (!isZnsL2(msg.getL2NetworkUuid())) {
        return;
    }

    String type = msg.getType();

    if (L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(type)) {
        // Flat 网络：无需额外要求

    } else if (L3NetworkConstant.L3_VPC_NETWORK_TYPE.equals(type)) {
        // VPC 网络：必须携带 znsTenantRouterUuid SystemTag
        String tenantRouterUuid = extractTenantRouterUuidFromSystemTags(msg.getSystemTags());
        if (tenantRouterUuid == null) {
            throw new ApiMessageInterceptionException(
                ZnsErrors.argerr(ZnsErrors.ZNS_VPC_REQUIRES_TENANT_ROUTER_TAG,
                    "creating VPC L3Network on ZNS L2 requires systemTag " +
                    "znsTenantRouterUuid::{uuid}")
            );
        }
        // 验证 TenantRouter 存在
        boolean exists = Q.New(ZnsTenantRouterVO.class)
            .eq(ZnsTenantRouterVO_.uuid, tenantRouterUuid)
            .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(
                ZnsErrors.argerr(ZnsErrors.ZNS_TENANT_ROUTER_NOT_FOUND,
                    "tenant router[uuid:%s] not found", tenantRouterUuid)
            );
        }

    } else {
        throw new ApiMessageInterceptionException(
            ZnsErrors.argerr(ZnsErrors.ZNS_L3_TYPE_NOT_SUPPORTED,
                "ZNS L2 only supports L3BasicNetwork or L3VpcNetwork, got type[%s]", type)
        );
    }

    // Category 限制：拒绝 Public / System 类别
    if (msg.getCategory() != null) {
        L3NetworkCategory cat = L3NetworkCategory.valueOf(msg.getCategory());
        if (cat == L3NetworkCategory.Public || cat == L3NetworkCategory.System) {
            throw new ApiMessageInterceptionException(
                ZnsErrors.argerr(ZnsErrors.ZNS_L3_CATEGORY_NOT_SUPPORTED,
                    "ZNS L2 does not support L3 category[%s]", cat)
            );
        }
    }
}

/** 从 SystemTag 列表中提取 znsTenantRouterUuid 的值 */
private String extractTenantRouterUuidFromSystemTags(List<String> systemTags) {
    if (systemTags == null) return null;
    for (String tag : systemTags) {
        if (ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID.isMatch(tag)) {
            return ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID
                .getTokenByTag(tag, ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID_TOKEN);
        }
    }
    return null;
}
```

#### 4.6.2 新增 VM 创建拦截（ZNS NIC 模式校验）

```java
private void validate(APICreateVmInstanceMsg msg) {
    for (String l3Uuid : msg.getL3NetworkUuids()) {
        if (!isZnsL3(l3Uuid)) {
            continue;
        }

        // 检查 VM systemTag 中的 znsNicMode
        String nicMode = extractZnsNicModeFromSystemTags(msg.getSystemTags());
        if (nicMode != null && !"dpdk".equalsIgnoreCase(nicMode)
                && !"kernel".equalsIgnoreCase(nicMode)) {
            throw new ApiMessageInterceptionException(
                ZnsErrors.argerr(ZnsErrors.ZNS_INVALID_NIC_MODE,
                    "invalid znsNicMode[%s], allowed values: dpdk | kernel", nicMode)
            );
        }

        // DPDK 模式：尽力检查候选主机是否有 ZNS dpdk 能力
        if ("dpdk".equalsIgnoreCase(nicMode)) {
            String controllerUuid = getZnsControllerUuidByL3(l3Uuid);
            if (controllerUuid != null) {
                boolean hasDpdkHost = Q.New(SdnControllerHostRefVO.class)
                    .eq(SdnControllerHostRefVO_.sdnControllerUuid, controllerUuid)
                    .isExists();
                if (!hasDpdkHost) {
                    throw new ApiMessageInterceptionException(
                        ZnsErrors.argerr(ZnsErrors.ZNS_DPDK_NO_CAPABLE_HOST,
                            "DPDK NIC requires at least one host managed by ZNS controller[uuid:%s]",
                            controllerUuid)
                    );
                }
            }
        }
    }
}

private String extractZnsNicModeFromSystemTags(List<String> systemTags) {
    if (systemTags == null) return null;
    for (String tag : systemTags) {
        if (ZnsSdnControllerSystemTags.ZNS_NIC_MODE.isMatch(tag)) {
            return ZnsSdnControllerSystemTags.ZNS_NIC_MODE
                .getTokenByTag(tag, ZnsSdnControllerSystemTags.ZNS_NIC_MODE_TOKEN);
        }
    }
    return null;
}
```

---

### 4.7 VM NIC 类型计算

#### 4.7.1 设计说明

**文件**：`VmNicManagerImpl.java`（位于 network-service 或 vm 插件）

方法 `computeVmNicType` 当前读取 L3 维度的 `ENABLE_DPDK_VHOSTUSER`，需改为读取 VM 维度的 `ZNS_NIC_MODE`，并保留旧 Tag fallback 读取逻辑（一个版本兼容窗口）。

#### 4.7.2 `computeVmNicType` 修改

> **赋值时机（P0-4）**：`computeVmNicType` 在 `VmNicManagerImpl.allocateNicForVm`（VM 创建流程的 NIC 分配步骤）中调用，结果写入 `VmNicVO.type` 并在同一事务内持久化。该步骤早于 `KVMRealizeL2GeneveNetworkBackend.realize()` 执行（realize 在后续 KVM 实现步骤中触发），因此 `nic.getType()` 在 `realize()` 入口处已有正确值。
>
> 实现时须在 `realize()` 入口添加断言日志：`logger.debug("realize nic[uuid:{}] type={}", nic.getUuid(), nic.getType())`，以便集成测试可观测 NIC 类型是否正确传递。

```java
/**
 * 计算 VM NIC 类型。
 * 对于 ZNS L2：优先读 VM 级别 znsNicMode Tag，fallback 到旧 L3 级别 enableDpdkVhostuser Tag。
 *
 * @param vmUuid    VM UUID
 * @param l2Network L2 网络 Inventory
 * @param l3Uuid    L3 网络 UUID
 * @return VmNicType
 */
public VmNicType computeVmNicType(String vmUuid,
                                   L2NetworkInventory l2Network,
                                   String l3Uuid) {
    // 非 ZNS L2 走原有逻辑
    if (!ZnsSdnControllerFactory.ZNS_TYPE.equals(l2Network.getvSwitchType())) {
        return defaultVmNicType(l2Network);
    }

    // ZNS L2：读取 NIC 模式
    String nicMode = ZnsSdnControllerSystemTags.resolveZnsNicMode(vmUuid, l3Uuid);

    if ("dpdk".equalsIgnoreCase(nicMode)) {
        // DPDK vhostuser 模式
        return VmNicType.valueOf(L2NetworkConstant.ACCEL_TYPE_VHOST_USER_SPACE);
    } else {
        // kernel 模式（默认）
        return VmNicType.valueOf(VmInstanceConstant.VIRTUAL_NIC_TYPE);
    }
}
```

---

### 4.8 DPDK NIC 生命周期

#### 4.8.1 设计说明

最小化路径：不引入新的 `VmNicLifecycle` 抽象框架，直接在 `KVMRealizeL2GeneveNetworkBackend` 中扩展，分支到 DPDK 路径。

Socket 路径约定：`/var/run/dpdk/vhost-{nicUuid}`（host 侧固定约定，避免传递配置）。

#### 4.8.2 新增 KVM Agent 命令（`KVMAgentCommands.java`）

```java
// 准备 DPDK vhostuser socket
public static class PrepareDpdkNicCmd extends AgentCommand {
    /** VM NIC UUID */
    public String vmNicUuid;
    /** host 侧 socket 路径，约定为 /var/run/dpdk/vhost-{nicUuid} */
    public String socketPath;
}

public static class PrepareDpdkNicResponse extends AgentResponse {
    // 无额外字段，通用 success/error 通过 AgentResponse 携带
}

// 释放 DPDK vhostuser socket
public static class ReleaseDpdkNicCmd extends AgentCommand {
    public String vmNicUuid;
    public String socketPath;
}

public static class ReleaseDpdkNicResponse extends AgentResponse {}
```

**KVM Agent 路径常量**（`KVMConstant.java`）：

```java
public static final String KVM_PREPARE_DPDK_NIC_PATH = "/prepareDpdkNic";
public static final String KVM_RELEASE_DPDK_NIC_PATH  = "/releaseDpdkNic";
```

#### 4.8.3 `KVMRealizeL2GeneveNetworkBackend` 扩展

**`realize` 方法（VM 创建/启动时）**：

```java
@Override
public void realize(L2NetworkInventory l2Network, String hostUuid,
                    VmNicInventory nic, Completion completion) {
    // 1. 原有 ZNS port 同步逻辑（createSegmentPort/updateSegmentPort）保持不变
    doZnsPortSync(l2Network, hostUuid, nic, new Completion(completion) {
        @Override
        public void success() {
            // P0-4: 断言日志，确认 NIC 类型在 realize() 前已正确写入
            logger.debug(String.format("realize nic[uuid:%s] type=%s",
                nic.getUuid(), nic.getType()));
            // 2. 根据 NIC 类型分支
            if (VmNicType.VHOSTUSER.toString().equals(nic.getType())) {
                prepareDpdkNic(hostUuid, nic, completion);
            } else {
                // kernel 模式：libvirt 默认路径，无需额外操作
                completion.success();
            }
        }
        @Override
        public void fail(ErrorCode err) { completion.fail(err); }
    });
}

private void prepareDpdkNic(String hostUuid, VmNicInventory nic,
                             Completion completion) {
    KVMAgentCommands.PrepareDpdkNicCmd cmd = new KVMAgentCommands.PrepareDpdkNicCmd();
    cmd.vmNicUuid  = nic.getUuid();
    cmd.socketPath = String.format("/var/run/dpdk/vhost-%s", nic.getUuid());

    kvmHostFactory.getConnector().sendCommand(hostUuid,
        KVMConstant.KVM_PREPARE_DPDK_NIC_PATH, cmd,
        KVMAgentCommands.PrepareDpdkNicResponse.class,
        new ReturnValueCompletion<KVMAgentCommands.PrepareDpdkNicResponse>(completion) {
            @Override
            public void success(KVMAgentCommands.PrepareDpdkNicResponse resp) {
                if (!resp.isSuccess()) {
                    completion.fail(ZnsErrors.operr(
                        "failed to prepare DPDK NIC[uuid:%s] on host[uuid:%s]: %s",
                        nic.getUuid(), hostUuid, resp.getError()));
                    return;
                }
                completion.success();
            }
            @Override
            public void fail(ErrorCode err) { completion.fail(err); }
        });
}
```

**`releaseOnVmDestroy` 方法扩展（VM 销毁时）**：

```java
@Override
public void releaseOnVmDestroy(L2NetworkInventory l2Network, VmInstanceInventory vm,
                               VmNicInventory nic, NoErrorCompletion completion) {
    // 原有清理逻辑（deleteSegmentPort 等）保持不变
    doZnsPortRelease(l2Network, vm.getHostUuid(), nic, new NoErrorCompletion(completion) {
        @Override
        public void done() {
            // DPDK 模式需要额外释放 socket
            if (VmNicType.VHOSTUSER.toString().equals(nic.getType())) {
                releaseDpdkNic(vm.getHostUuid(), nic, completion);
            } else {
                completion.done();
            }
        }
    });
}

private void releaseDpdkNic(String hostUuid, VmNicInventory nic,
                             NoErrorCompletion completion) {
    KVMAgentCommands.ReleaseDpdkNicCmd cmd = new KVMAgentCommands.ReleaseDpdkNicCmd();
    cmd.vmNicUuid  = nic.getUuid();
    cmd.socketPath = String.format("/var/run/dpdk/vhost-%s", nic.getUuid());

    kvmHostFactory.getConnector().sendCommand(hostUuid,
        KVMConstant.KVM_RELEASE_DPDK_NIC_PATH, cmd,
        KVMAgentCommands.ReleaseDpdkNicResponse.class,
        new ReturnValueCompletion<KVMAgentCommands.ReleaseDpdkNicResponse>(completion) {
            @Override
            public void success(KVMAgentCommands.ReleaseDpdkNicResponse resp) {
                if (!resp.isSuccess()) {
                    // release 失败记录 warning，不阻断 VM 删除流程
                    logger.warn(String.format(
                        "[release-dpdk-nic] failed to release DPDK NIC[uuid:%s] " +
                        "on host[uuid:%s]: %s", nic.getUuid(), hostUuid, resp.getError()));
                }
                completion.done(); // 无论成功失败都继续
            }
            @Override
            public void fail(ErrorCode err) {
                logger.warn(String.format(
                    "[release-dpdk-nic] error releasing DPDK NIC[uuid:%s]: %s",
                    nic.getUuid(), err.getDetails()));
                completion.done();
            }
        });
}
```

#### 4.8.4 OVS Kernel 路径验证要点

- `KVMRealizeL2GeneveNetworkBackend.realize` 中，当 `nic.getType()` 为 `NONE`（kernel）时，不应调用任何 dpdk 相关方法
- 验证方式：单元测试 mock `VmNicVO.type = NONE`，断言 `PrepareDpdkNicCmd` 未被发送
- **预期结论**：无代码改动，仅添加验证测试

---

### 4.9 Reconcile SoT 拆分

#### 4.9.1 设计说明

`reconnectSdnController` 原来是单一 FlowChain，将其重构为两个逻辑组：
- **Group A**（ZNS-as-SoT）：设备资源（TZ、TN、Tenant、TenantRouter）从 ZNS 全量同步到 Cloud
- **Group B**（Cloud-as-SoT）：Segment（L2/L3 对应的 ZNS Segment）由 Cloud 主导三路对比

#### 4.9.2 `syncDeviceResourcesFromZns`

```java
/**
 * Group A: ZNS-as-SoT 设备资源同步。
 * 执行顺序：TZ → TN → Tenant → TenantRouter
 * 每一步：从 ZNS 拉取全量 → Upsert 到 DB → 删除 DB 中 ZNS 不存在的孤儿记录
 */
private void syncDeviceResourcesFromZns(String controllerUuid, String znsIp,
                                         String scope, Completion completion) {
    FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
    chain.setName(String.format("sync-device-resources-from-zns-%s", controllerUuid));
    chain.allowEmptyFlow();

    if (shouldSync(scope, "TZ", "ALL")) {
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-transport-zones-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTransportZonesFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });
    }

    if (shouldSync(scope, "TN", "ALL")) {
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-transport-nodes-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTransportNodesFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });
    }

    if (shouldSync(scope, "ALL")) {
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-tenants-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTenantsFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) {
                        logger.warn("tenant API unavailable, skipping: " + err.getDetails());
                        trigger.next();
                    }
                });
            }
        });

        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-tenant-routers-zns-as-sot";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncTenantRoutersFromZns(znsIp, controllerUuid, new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
            }
        });
    }

    chain.done(new FlowDoneHandler(completion) {
        @Override public void handle(Map data) { completion.success(); }
    }).error(new FlowErrorHandler(completion) {
        @Override public void handle(ErrorCode err, Map data) { completion.fail(err); }
    }).start();
}

private boolean shouldSync(String scope, String... targets) {
    if (scope == null || "ALL".equalsIgnoreCase(scope)) return true;
    for (String t : targets) {
        if (t.equalsIgnoreCase(scope)) return true;
    }
    return false;
}
```

> **scope 值映射**：`FABRIC` → TZ+TN；`TENANT` → Tenant+TenantRouter。  
> `shouldSync(scope, "TZ", "FABRIC")` 对 scope=FABRIC 返回 true，对 scope=TENANT 返回 false。
```

#### 4.9.3 新 `reconnectSdnController`

```java
@Override
public void reconnectSdnController(ReconnectSdnControllerMsg msg, NoErrorCompletion completion) {
    SdnControllerVO controller = dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class);
    String znsIp = controller.getIp();

    FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
    chain.setName(String.format("reconnect-zns-controller-%s", controller.getUuid()));

    // Group A: ZNS-as-SoT 设备资源
    chain.then(new NoRollbackFlow() {
        String __name__ = "group-a-sync-device-resources";
        @Override
        public void run(FlowTrigger trigger, Map data) {
            syncDeviceResourcesFromZns(controller.getUuid(), znsIp, "ALL",
                new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) {
                        // Group A 失败记录日志但不阻断 Group B
                        logger.warn("[reconnect] Group A sync failed: " + err.getDetails());
                        trigger.next();
                    }
                });
        }
    });

    // Group B: Cloud-as-SoT Segment 三路对比
    chain.then(new NoRollbackFlow() {
        String __name__ = "group-b-reconcile-segments-cloud-as-sot";
        @Override
        public void run(FlowTrigger trigger, Map data) {
            reconcileSegmentsCloudAsSoT(controller.getUuid(), znsIp,
                new Completion(trigger) {
                    @Override public void success() { trigger.next(); }
                    @Override public void fail(ErrorCode err) { trigger.fail(err); }
                });
        }
    });

    chain.done(new FlowDoneHandler(null) {
        @Override public void handle(Map data) { completion.done(); }
    }).error(new FlowErrorHandler(null) {
        @Override public void handle(ErrorCode err, Map data) {
            logger.error("[reconnect] FlowChain failed: " + err.getDetails());
            completion.done();
        }
    }).start();
}
```

#### 4.9.4 TenantRouter 同步中的边界处理

`syncTenantRoutersFromZns` 内部的孤儿处理逻辑：

```java
private void syncTenantRoutersFromZns(String znsIp, String controllerUuid,
                                       Completion completion) {
    znsApiClient.listTenantRouters(znsIp,
        new ReturnValueCompletion<ListResponse<ZnsApiCommands.TenantRouterData>>(completion) {
            @Override
            public void success(ListResponse<ZnsApiCommands.TenantRouterData> resp) {
                List<ZnsApiCommands.TenantRouterData> znsRouters = resp.getResults();

                // Upsert：ZNS 有 → 写入或更新 Cloud DB
                Set<String> znsExternalUuids = new HashSet<>();
                for (ZnsApiCommands.TenantRouterData rd : znsRouters) {
                    znsExternalUuids.add(rd.uuid);
                    upsertTenantRouter(controllerUuid, rd);
                }

                // 孤儿处理：Cloud 有但 ZNS 无
                List<ZnsTenantRouterVO> cloudRouters = Q.New(ZnsTenantRouterVO.class)
                    .eq(ZnsTenantRouterVO_.sdnControllerUuid, controllerUuid)
                    .list();
                for (ZnsTenantRouterVO cloudRouter : cloudRouters) {
                    if (!znsExternalUuids.contains(cloudRouter.getExternalUuid())) {
                        handleOrphanTenantRouter(cloudRouter);
                    }
                }
                completion.success();
            }
            @Override
            public void fail(ErrorCode err) { completion.fail(err); }
        });
}

private void handleOrphanTenantRouter(ZnsTenantRouterVO orphan) {
    String tagPattern = ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID
        .instantiateTag(map(e(
            ZnsSdnControllerSystemTags.ZNS_TENANT_ROUTER_UUID_TOKEN, orphan.getUuid()
        )));
    boolean l3InUse = Q.New(SystemTagVO.class)
        .eq(SystemTagVO_.tag, tagPattern)
        .eq(SystemTagVO_.resourceType, L3NetworkVO.class.getSimpleName())
        .isExists();

    if (l3InUse) {
        // 有 L3 引用：触发告警，不删除
        evtf.fire(ZnsCanonicalEvents.EVT_ZNS_TENANT_ROUTER_ORPHAN_IN_CLOUD,
            new ZnsCanonicalEvents.TenantRouterOrphanData(
                orphan.getUuid(),
                String.format(
                    "ZNS deleted tenant router[uuid:%s] but Cloud VPC L3 networks " +
                    "still reference it. Please delete the L3 networks first.",
                    orphan.getUuid())
            ));
    } else {
        // 无引用：静默删除
        dbf.remove(orphan);
    }
}
```

---

### 4.10 运行时推送通知处理

#### 4.10.1 `SyncFabricHandler`

```java
class SyncFabricHandler {

    public String handle(ZnsNotificationCommands.SyncFabricNotification cmd) {

        SdnControllerVO controller = findControllerByComputerManagerUuid(cmd.computerManagerUuid);
        if (controller == null) {
            // P1-1: 返回结构化错误响应，不抛异常
            ZnsNotificationCommands.NotificationResponse errResp =
                new ZnsNotificationCommands.NotificationResponse();
            errResp.success = false;
            errResp.error   = String.format(
                "no SDN controller found for computerManagerUuid[%s]",
                cmd.computerManagerUuid);
            return JSONObjectUtil.toJsonString(errResp);
        }

        String scope = cmd.scope != null ? cmd.scope : "ALL";
        final String controllerUuid = controller.getUuid();
        final String znsIp          = controller.getIp();

        // P1-2: 使用 per-controller 异步串行化队列，防止并发推送时 reconcile 竞争。
        //       同一 controllerUuid 的请求排队执行，不并发。
        thdf.chainSubmit(new ChainTask(null) {
            @Override public String getSyncSignature() {
                return String.format("zns-sync-fabric-%s", controllerUuid);
            }
            @Override public void run(SyncTaskChain chain) {
                syncDeviceResourcesFromZns(controllerUuid, znsIp, scope,
                    new Completion(null) {
                        @Override public void success() {
                            logger.info(String.format("[sync-fabric] completed for " +
                                "controller[uuid:%s] scope[%s]", controllerUuid, scope));
                            chain.next();
                        }
                        @Override public void fail(ErrorCode err) {
                            logger.error(String.format("[sync-fabric] failed for " +
                                "controller[uuid:%s]: %s", controllerUuid, err.getDetails()));
                            chain.next();
                        }
                    });
            }
            @Override public String getName() { return getSyncSignature(); }
        });

        // 202 Accepted：已受理，异步执行
        ZnsNotificationCommands.NotificationResponse resp =
            new ZnsNotificationCommands.NotificationResponse();
        resp.success = true;
        return JSONObjectUtil.toJsonString(resp);
    }
}
```

#### 4.10.2 `SyncResourceHandler`

```java
class SyncResourceHandler {

    public String handle(ZnsNotificationCommands.SyncResourceNotification cmd) {

        SdnControllerVO controller = findControllerByComputerManagerUuid(cmd.computerManagerUuid);
        if (controller == null) {
            // P1-1: 结构化错误响应
            ZnsNotificationCommands.NotificationResponse errResp =
                new ZnsNotificationCommands.NotificationResponse();
            errResp.success = false;
            errResp.error   = String.format(
                "no SDN controller found for computerManagerUuid[%s]",
                cmd.computerManagerUuid);
            return JSONObjectUtil.toJsonString(errResp);
        }

        final SdnControllerVO ctrl = controller;

        // P1-2: per-controller 串行化
        thdf.chainSubmit(new ChainTask(null) {
            @Override public String getSyncSignature() {
                return String.format("zns-sync-resource-%s", ctrl.getUuid());
            }
            @Override public void run(SyncTaskChain chain) {
                if ("TENANT_ROUTER".equalsIgnoreCase(cmd.resourceType)) {
                    handleTenantRouterEvent(ctrl, cmd, chain);
                } else if ("SEGMENT".equalsIgnoreCase(cmd.resourceType)) {
                    handleSegmentEvent(ctrl, cmd, chain);
                } else {
                    logger.warn(String.format("[sync-resource] unknown resourceType[%s], ignored",
                        cmd.resourceType));
                    chain.next();
                }
            }
            @Override public String getName() { return getSyncSignature(); }
        });

        ZnsNotificationCommands.NotificationResponse resp =
            new ZnsNotificationCommands.NotificationResponse();
        resp.success = true;
        return JSONObjectUtil.toJsonString(resp);
    }

    private void handleTenantRouterEvent(SdnControllerVO controller,
                                          ZnsNotificationCommands.SyncResourceNotification cmd,
                                          SyncTaskChain chain) {
        if ("CREATE".equalsIgnoreCase(cmd.action)
                || "UPDATE".equalsIgnoreCase(cmd.action)) {
            znsApiClient.getTenantRouter(controller.getIp(), cmd.resourceUuid,
                new ReturnValueCompletion<GetResponse<ZnsApiCommands.TenantRouterData>>(null) {
                    @Override
                    public void success(GetResponse<ZnsApiCommands.TenantRouterData> resp) {
                        upsertTenantRouter(controller.getUuid(), resp.getResult());
                        chain.next();
                    }
                    @Override
                    public void fail(ErrorCode err) {
                        logger.error(String.format(
                            "[sync-resource] failed to get tenant router[externalUuid:%s]: %s",
                            cmd.resourceUuid, err.getDetails()));
                        chain.next();
                    }
                });

        } else if ("DELETE".equalsIgnoreCase(cmd.action)) {
            ZnsTenantRouterVO vo = Q.New(ZnsTenantRouterVO.class)
                .eq(ZnsTenantRouterVO_.externalUuid, cmd.resourceUuid)
                .find();
            if (vo != null) {
                handleOrphanTenantRouter(vo);
            }
            chain.next();
        } else {
            chain.next();
        }
    }

    private void handleSegmentEvent(SdnControllerVO controller,
                                     ZnsNotificationCommands.SyncResourceNotification cmd,
                                     SyncTaskChain chain) {
        reconcileSegmentsCloudAsSoT(controller.getUuid(), controller.getIp(),
            new Completion(null) {
                @Override
                public void success() {
                    logger.info(String.format("[sync-resource/segment] reconcile completed " +
                        "for controller[uuid:%s]", controller.getUuid()));
                    chain.next();
                }
                @Override
                public void fail(ErrorCode err) {
                    logger.error(String.format("[sync-resource/segment] reconcile failed " +
                        "for controller[uuid:%s]: %s", controller.getUuid(), err.getDetails()));
                    chain.next();
                }
            });
    }
}
```

---

### 4.11 DHCP/DNS/MTU 完整性

#### 4.11.1 双栈 DHCP

**文件**：`ZnsSdnControllerDhcp.java`

**`enableDHCP` 验证要点**：

```java
// 确认 buildDhcpConfig 同时处理 v4 和 v6 IpRange
private ZnsApiCommands.DhcpServiceConfig buildDhcpConfig(
        String segmentUuid, L3NetworkInventory l3) {

    ZnsApiCommands.DhcpServiceConfig config = new ZnsApiCommands.DhcpServiceConfig();
    config.segmentUuid = segmentUuid;

    for (IpRangeInventory ipRange : l3.getIpRanges()) {
        if (NetworkUtils.isIpv4Address(ipRange.getGateway())) {
            // IPv4 配置
            config.gateway_address = ipRange.getGateway();
            config.subnet_mask     = ipRange.getNetmask();
            // ... 其他 v4 字段
        } else {
            // IPv6 配置
            config.gateway6_address = ipRange.getGateway();
            config.subnet6_prefix   = ipRange.getPrefixLen();
            // ... 其他 v6 字段
        }
    }
    return config;
}
```

**边界场景：先启用 v4 DHCP，后添加 v6 IpRange**：

在 `ZnsControllerManager.afterCreateIpRange` 中：

```java
@Override
public void afterCreateIpRange(IpRangeInventory ipRange) {
    // 若 L3 所在 L2 是 ZNS，且 DHCP 已启用，则触发 DHCP 服务更新
    L3NetworkVO l3 = dbf.findByUuid(ipRange.getL3NetworkUuid(), L3NetworkVO.class);
    if (l3 == null || !isDhcpEnabled(l3.getUuid())) {
        return;
    }
    if (!isZnsL3(l3.getUuid())) {
        return;
    }
    SdnControllerUpdateDHCPMsg updateMsg = new SdnControllerUpdateDHCPMsg();
    updateMsg.setL3NetworkUuid(l3.getUuid());
    bus.makeLocalServiceId(updateMsg, SdnControllerConstant.SERVICE_ID);
    bus.send(updateMsg);
}
```

#### 4.11.2 DNS 独立更新

`ZnsSdnControllerDhcp.updateDHCP` 在处理 DNS 变更时，只修改 `dns_servers` 字段：

```java
private ZnsApiCommands.DhcpServicePatch buildDnsPatch(List<String> dnsServers) {
    ZnsApiCommands.DhcpServicePatch patch = new ZnsApiCommands.DhcpServicePatch();
    patch.dns_servers = dnsServers;
    // 不包含 dhcp_configs（不改变 DHCP 启停状态）
    return patch;
}
```

**验证要点**：`APIUpdateL3NetworkMsg` 变更 DNS → 调用 `SdnControllerUpdateDHCPMsg` → ZNS PATCH 只含 `dns_servers`，不触发 DHCP enable/disable。

#### 4.11.3 MTU 更新（已实现，验证）

`ZnsControllerManager.afterSetL3NetworkMtu` → PATCH segment `{mtu: value}`。

验证：双栈 L3（同一 Segment）MTU 更新只需 PATCH 一次，两个 IpRange 共享同一 segmentUuid。

---

### 4.12 双栈 IpRange

#### 4.12.1 设计说明

`ZnsSdnControllerL3.createIpRange` 已区分 v4/v6，分别设置 `gateway_address` / `gateway6_address`。需验证以下边界场景。

#### 4.12.2 验证矩阵

| 场景 | 预期行为 | 是否需要代码改动 |
|------|----------|-----------------|
| L3 仅有 v4 IpRange | PATCH segment 含 `gateway_address`，无 `gateway6_address` | 否 |
| L3 仅有 v6 IpRange | PATCH segment 含 `gateway6_address`，无 `gateway_address` | 否 |
| L3 同时有 v4 + v6 IpRange | PATCH segment 同时含两个字段，v4 不覆盖 v6 | 验证 |
| 添加 v6 后不覆盖 v4 | `createIpRange` 做 partial update（PATCH 非 PUT） | 验证 |
| 删除 v4，保留 v6 | v6 配置不受影响；若 DHCP 启用则拦截（ZnsApiInterceptor） | 验证 |

**reconcile `needsSegmentUpdate` 需比较双栈字段**：

```java
/**
 * P1-3: gateway/MTU 是 L3/IpRange 层概念，不在 L2NetworkInventory 上。
 * 方法签名改为接受 L3NetworkInventory，从 IpRange 列表取 gateway 比较。
 */
private boolean needsSegmentUpdate(ZnsApiCommands.SegmentData znsSegment,
                                    L3NetworkInventory cloudL3) {
    String gwV4 = null, gwV6 = null;
    for (IpRangeInventory ipRange : cloudL3.getIpRanges()) {
        if (NetworkUtils.isIpv4Address(ipRange.getGateway())) {
            gwV4 = ipRange.getGateway();
        } else {
            gwV6 = ipRange.getGateway();
        }
    }
    // 比较 v4 字段
    if (!Objects.equals(znsSegment.gateway_address, gwV4)) return true;
    // 比较 v6 字段
    if (!Objects.equals(znsSegment.gateway6_address, gwV6)) return true;
    // MTU 从 L3 的 systemTag 或 L2 基础属性取，ZStack 惯例通常存于 L3 层 tag
    // 若 ZNS segment 携带 mtu 字段且 Cloud L3 有对应 tag，则比较
    return false;
}
```

---

### 4.13 告警事件（TenantRouter 孤儿）

**新增规范事件常量**（`ZnsCanonicalEvents.java`）：

```java
public class ZnsCanonicalEvents {

    public static final String EVT_ZNS_TENANT_ROUTER_ORPHAN_IN_CLOUD =
        "/zns/tenantRouter/orphanInCloud";

    @EventDefinition
    public static class TenantRouterOrphanData {
        public String tenantRouterUuid;
        public String message;

        public TenantRouterOrphanData(String uuid, String msg) {
            this.tenantRouterUuid = uuid;
            this.message          = msg;
        }
    }
}
```

此事件可触发 Cloud 告警系统（AlarmManager）向运维人员发送通知。告警内容：

> ZNS 侧已删除租户路由器 [uuid:%s]，但 Cloud 中仍有 VPC L3 网络引用该路由器。请先删除对应的 L3 网络，再重新触发同步。

---

### 4.14 错误码

**文件**：`ZnsErrors.java`（或 `ZnsErrorCode.java`）

```java
public interface ZnsErrors {

    // ===== 已有错误码（保持不变）=====
    // ...

    // ===== 新增错误码 =====

    /** VPC L3 在 ZNS L2 上必须携带 znsTenantRouterUuid systemTag */
    String ZNS_VPC_REQUIRES_TENANT_ROUTER_TAG = "ORG_ZSTACK_NETWORK_ZNS_10020";

    /** 指定的租户路由器不存在 */
    String ZNS_TENANT_ROUTER_NOT_FOUND = "ORG_ZSTACK_NETWORK_ZNS_10021";

    /** ZNS L2 不支持该 L3 类别（Public/System） */
    String ZNS_L3_CATEGORY_NOT_SUPPORTED = "ORG_ZSTACK_NETWORK_ZNS_10022";

    /** 无法删除租户路由器，仍有 L3 网络引用 */
    String ZNS_TENANT_ROUTER_IN_USE = "ORG_ZSTACK_NETWORK_ZNS_10023";

    /** 找不到对应 computerManagerUuid 的 SDN 控制器 */
    String ZNS_CONTROLLER_NOT_FOUND_FOR_CM_UUID = "ORG_ZSTACK_NETWORK_ZNS_10024";

    /** DPDK NIC 需要有 ZNS dpdk 能力的主机 */
    String ZNS_DPDK_NO_CAPABLE_HOST = "ORG_ZSTACK_NETWORK_ZNS_10025";

    /** L3 类型不被 ZNS L2 支持 */
    String ZNS_L3_TYPE_NOT_SUPPORTED = "ORG_ZSTACK_NETWORK_ZNS_10011";

    // 工具方法（可直接调用）
    static ErrorCode argerr(String code, String fmt, Object... args) {
        return Platform.argerr(code, fmt, args);
    }

    static ErrorCode operr(String code, String fmt, Object... args) {
        return Platform.operr(code, fmt, args);
    }
}
```

**错误码 i18n 文件**（`zns-errors.properties`）：

```properties
ORG_ZSTACK_NETWORK_ZNS_10020=在 ZNS L2 上创建 VPC 类型 L3 网络时，必须提供 systemTag [znsTenantRouterUuid::{uuid}]
ORG_ZSTACK_NETWORK_ZNS_10021=租户路由器 [uuid:{0}] 不存在
ORG_ZSTACK_NETWORK_ZNS_10022=ZNS L2 不支持 L3 网络类别 [{0}]，仅支持 Private
ORG_ZSTACK_NETWORK_ZNS_10023=无法删除租户路由器 [uuid:{0}]，以下 L3 网络仍在引用：{1}
ORG_ZSTACK_NETWORK_ZNS_10024=找不到 computerManagerUuid [{0}] 对应的 SDN 控制器
ORG_ZSTACK_NETWORK_ZNS_10025=DPDK NIC 模式需要至少一台由 ZNS 控制器 [uuid:{0}] 管理的主机
```

---

## 5. 新增错误码清单

| 错误码 | 常量名 | 场景 | 类型 |
|--------|--------|------|------|
| `ORG_ZSTACK_NETWORK_ZNS_10020` | `ZNS_VPC_REQUIRES_TENANT_ROUTER_TAG` | 创建 VPC L3 未携带 TenantRouter Tag | argerr |
| `ORG_ZSTACK_NETWORK_ZNS_10021` | `ZNS_TENANT_ROUTER_NOT_FOUND` | 指定 TenantRouter 不存在 | argerr |
| `ORG_ZSTACK_NETWORK_ZNS_10022` | `ZNS_L3_CATEGORY_NOT_SUPPORTED` | L3 Category 不支持（Public/System） | argerr |
| `ORG_ZSTACK_NETWORK_ZNS_10023` | `ZNS_TENANT_ROUTER_IN_USE` | 删除 TenantRouter 时仍有 L3 引用 | operr |
| `ORG_ZSTACK_NETWORK_ZNS_10024` | `ZNS_CONTROLLER_NOT_FOUND_FOR_CM_UUID` | 反向通知时找不到控制器 | operr |
| `ORG_ZSTACK_NETWORK_ZNS_10025` | `ZNS_DPDK_NO_CAPABLE_HOST` | DPDK 模式无可用主机 | argerr |

---

## 6. Flyway DDL 清单

| 文件名 | 内容 | 依赖 |
|--------|------|------|
| `V5.0.x__ZnsTenantVO.sql` | 创建 `ZnsTenantVO` 表 | 无 |
| `V5.0.x__ZnsTenantRouterVO.sql` | 创建 `ZnsTenantRouterVO` 表 | 无（tenantUuid 可为 NULL） |

**注意事项**：
- Flyway 文件版本号 `5.0.x` 中的 `x` 需根据 ZStack 版本号规范确定具体序号
- 两个表不设外键约束（ZStack 惯例，由应用层保证引用完整性）
- `externalUuid` 加 `UNIQUE KEY`，确保 ZNS 侧 UUID 不重复导入

---

## 7. SystemTag 变更清单

| 旧 Tag | 挂载维度 | 新 Tag | 挂载维度 | 变更类型 | 迁移说明 |
|--------|---------|--------|---------|---------|---------|
| `enableDpdkVhostuser::{enableDpdkVhostuser}` | `L3NetworkVO` | `znsNicMode::{mode}` | `VmInstanceVO` | 重命名 + 维度变更 | 新特性 Tag，无历史数据；旧 Tag 加 `@Deprecated`，保留一个 minor 版本作为 fallback 读取，v5.1 删除 |
| ——（新增）——  | —— | `znsTenantRouterUuid::{tenantRouterUuid}` | `L3NetworkVO` | 新增 | VPC L3 创建时必须携带 |

---

## 8. 实现顺序建议

### Wave 1 — 基础数据模型（无业务依赖）

| 任务 | 说明 |
|------|------|
| 创建 `ZnsTenantVO` + Flyway DDL | 基础 VO，不含业务逻辑 |
| 创建 `ZnsTenantRouterVO` + Flyway DDL | 基础 VO，不含业务逻辑 |
| `ZnsTenantInventory` / `ZnsTenantRouterInventory` | Inventory 类 |
| `APIQueryZnsTenantRouterMsg` / Reply | Query API |
| 更新 `ZnsSdnControllerSystemTags`（新增 `ZNS_NIC_MODE`, `ZNS_TENANT_ROUTER_UUID`，废弃旧 Tag） | 无运行时副作用 |
| 新增错误码（`ZnsErrors`） | 常量类，零依赖 |

### Wave 2 — ZNS API Client 扩展

| 任务 | 说明 |
|------|------|
| `ZnsApiCommands.TenantData` / `TenantRouterData` DTO | 无业务逻辑 |
| `ZnsApiClient.listTenantRouters` | 新增接口方法 |
| `ZnsApiClient.getTenantRouter` | 新增接口方法 |

### Wave 3 — 反向通知通道

| 任务 | 说明 |
|------|------|
| `ZnsNotificationCommands`（所有 Notification DTO） | 数据类 |
| `ZnsNotificationController`（Spring @Controller，4 个 @RequestMapping 端点） | 核心新增类 |
| `ZnsConstant` 新增 HTTP 路径常量 | 常量 |

### Wave 4 — Add-CM / Wizard Init 流程

| 任务 | 说明 |
|------|------|
| `AddComputeManagerHandler` 实现 | 依赖 Wave 1/3 |
| `WizardInitSyncHandler` 实现（含 5 个 Flow） | 依赖 Wave 2/3 |
| `ZnsSdnController.initSdnController` 移除 Flow 5 | 手术式删除 |
| `ZnsSdnControllerConstant.MANUAL_ADD_DEPRECATION_NOTE` | 文档常量 |

### Wave 5 — Reconcile 重构 + 运行时通知

| 任务 | 说明 |
|------|------|
| `syncDeviceResourcesFromZns`（提取为独立方法） | 依赖 Wave 2/4 |
| `syncTenantRoutersFromZns` + 孤儿处理逻辑 | 依赖 Wave 1 |
| `ZnsCanonicalEvents.EVT_ZNS_TENANT_ROUTER_ORPHAN_IN_CLOUD` | 事件常量 |
| 新 `reconnectSdnController`（Group A + Group B） | 重构现有方法 |
| `SyncFabricHandler` | 依赖 Wave 3/5 |
| `SyncResourceHandler` | 依赖 Wave 3/5 |

### Wave 6 — API 拦截 + VM/NIC 层

| 任务 | 说明 |
|------|------|
| `ZnsApiInterceptor.validate(APICreateL3NetworkMsg)` — VPC 支持 | 依赖 Wave 1 |
| `ZnsApiInterceptor.validate(APIDeleteZnsTenantRouterMsg)` — 限制删除 | 依赖 Wave 1 |
| `ZnsApiInterceptor.validate(APICreateVmInstanceMsg)` — NIC 模式校验 | 依赖 Wave 1 |
| `VmNicManagerImpl.computeVmNicType` 改为读 VM 维度 Tag | 依赖 Wave 1 |
| `KVMAgentCommands` 新增 `PrepareDpdkNicCmd` / `ReleaseDpdkNicCmd` | 依赖无 |
| `KVMRealizeL2GeneveNetworkBackend` DPDK 分支 | 依赖 Wave 1/6 |
| DHCP/DNS/MTU 完整性验证 + `afterCreateIpRange` 修复 | 依赖 Wave 4 |
| 双栈 IpRange `needsSegmentUpdate` 双字段比较 | 回归验证 |

---

## 附录 A：关键接口/类一览

| 类/接口 | 所在包 | 变更类型 |
|---------|--------|---------|
| `ZnsNotificationController` | `org.zstack.network.zns` | **新增** |
| `ZnsNotificationCommands` | `org.zstack.network.zns` | **新增** |
| `ZnsCanonicalEvents` | `org.zstack.network.zns` | **新增** |
| `ZnsTenantVO` | `org.zstack.network.zns` | **新增** |
| `ZnsTenantRouterVO` | `org.zstack.network.zns` | **新增** |
| `ZnsTenantInventory` | `org.zstack.network.zns` | **新增** |
| `ZnsTenantRouterInventory` | `org.zstack.network.zns` | **新增** |
| `APIQueryZnsTenantRouterMsg` | `org.zstack.network.zns` | **新增** |
| `ZnsErrors` | `org.zstack.network.zns` | **修改**（新增 6 个常量） |
| `ZnsSdnControllerSystemTags` | `org.zstack.network.zns` | **修改**（废弃 1 个，新增 2 个） |
| `ZnsApiInterceptor` | `org.zstack.network.zns` | **修改**（VPC 支持 + 新拦截） |
| `ZnsSdnController` | `org.zstack.network.zns` | **修改**（移除 Flow 5，重构 reconnect） |
| `ZnsApiCommands` | `org.zstack.network.zns` | **修改**（新增 DTO） |
| `ZnsApiClient` | `org.zstack.network.zns` | **修改**（新增 2 个方法） |
| `ZnsConstant` | `org.zstack.network.zns` | **修改**（新增 HTTP 路径常量） |
| `ZnsSdnControllerConstant` | `org.zstack.network.zns` | **修改**（新增废弃常量） |
| `VmNicManagerImpl` | `org.zstack.network.vm`（或 network-service） | **修改**（NIC 类型计算） |
| `KVMAgentCommands` | `org.zstack.kvm` | **修改**（新增 DPDK 命令） |
| `KVMRealizeL2GeneveNetworkBackend` | `org.zstack.kvm` | **修改**（DPDK 分支） |
| `KVMConstant` | `org.zstack.kvm` | **修改**（新增路径常量） |
| `ZnsSdnControllerDhcp` | `org.zstack.network.zns` | **验证**（DHCP 完整性） |
| `ZnsSdnControllerL3` | `org.zstack.network.zns` | **验证**（双栈 IpRange） |

---

## 附录 B：数据流图

### B.1 新 Add-CM 流程

```
ZNS Wizard
    │
    │ POST /zns/notify/add-compute-manager
    │ {vip, account, password, computerManagerUuid, name}
    ▼
ZnsNotificationController.AddComputeManagerHandler
    │
    ├─ 幂等检查（computerManagerUuid）
    ├─ persistNewController @Transactional
    │      persist SdnControllerVO
    │      persist ZnsControllerVO
    │      create SystemTag computerManagerUuid::{uuid}
    ├─ fire SDN_CONTROLLER_ADDED event
    │
    └─ return {uuid, success:true}
         │
         ▼
       ZNS 存储 Cloud 侧 controller UUID
```

### B.2 Wizard Init Sync 流程

```
ZNS Wizard（T1 创建完成）
    │
    │ POST /zns/notify/wizard-init-sync {computerManagerUuid}
    ▼
WizardInitSyncHandler
    │
    ├─ Flow 1: sync-transport-zones-zns-as-sot
    │     pull TZ from ZNS → upsert ZnsTransportZoneVO → delete orphans
    ├─ Flow 2: sync-transport-nodes-zns-as-sot
    │     pull TN → upsert SdnControllerHostRefVO → delete orphans
    ├─ Flow 3: sync-tenants-zns-as-sot（optional）
    │     pull Tenant → upsert ZnsTenantVO → delete orphans
    ├─ Flow 4: sync-tenant-routers-zns-as-sot
    │     pull TR → upsert ZnsTenantRouterVO → handle orphans
    └─ Flow 5: cleanup-orphan-segments
          list ZNS Segments → find no-Cloud-L2 counterpart → deleteSegment(force=true)
```

### B.3 Reconcile SoT 拆分

```
reconnectSdnController
    │
    ├─ [Group A: ZNS-as-SoT] syncDeviceResourcesFromZns
    │       TZ sync → TN sync → Tenant sync → TenantRouter sync
    │       每步：pull all from ZNS → upsert → delete orphans
    │
    └─ [Group B: Cloud-as-SoT] reconcileSegmentsCloudAsSoT
            3-way diff: Cloud L2 ↔ ZNS Segment ↔ cloud-basis
            原有逻辑不变
```
