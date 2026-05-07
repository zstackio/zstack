# P3 详细设计 Review 报告

**文档**：ZCF-2133-zns-pivot-detailed-design.md v1.0.0  
**评审日期**：2025-07  
**评审人**：Senior Java/Cloud Platform Architect

---

## 总评

设计整体思路清晰，SoT 拆分逻辑合理，SystemTag 迁移方案保留了向后兼容窗口。但存在 4 个 P0 阻塞问题，其中两个（HTTP 机制选型错误、缺失依赖注入字段）将导致运行时 NPE 或 404，必须在进入 P4 实现前全部修复。

**总体结论：NEEDS REWORK**

---

## 统计

| 级别 | 数量 |
|------|------|
| P0 (阻塞) | 4 |
| P1 (重要) | 4 |
| P2 (次要) | 4 |

---

## P0 问题（阻塞，必须修复）

### P0-1：`registerSyncHttpCallHandler` 对外部 ZNS 系统不可用

**位置**：§4.1.2 `ZnsReverseNotificationFacade.start()`

**问题**：设计对 4 个 ZNS 通知端点全部使用 `RESTFacade.registerSyncHttpCallHandler`。该机制属于 ZStack 内部 Agent 协议——要求调用方在 HTTP 请求中携带 `commandpath` 头以完成路由分发。ZNS 是外部系统，其 POST 请求不携带任何 `commandpath` 头（参见现有 `ZnsCallbackController.java` 的注释："ZNS is an external system that simply POSTs a JSON body to the webhook URL without extra routing headers"）。使用此机制后 ZNS 推送的请求将全部被 RESTFacade dispatch 忽略或返回 404，通知通道完全失效。

**建议**：与现有 `ZnsCallbackController.java` 保持一致，新建或扩展一个 Spring `@Controller`（如 `ZnsNotificationController`），对每个端点使用 `@RequestMapping` 注册处理方法，接收 `@RequestBody` 后分发给各 Handler。不需要 `registerSyncHttpCallHandler`。

---

### P0-2：`ZnsReverseNotificationFacade` 缺失 `evtf` 和 `znsApiClient` 依赖注入

**位置**：§4.1.2 外部类声明；§4.2.2 `AddComputeManagerHandler` step 5；§4.10.2 `SyncResourceHandler.handleTenantRouterEvent`

**问题**：外部类 `ZnsReverseNotificationFacade` 的 `@Autowired` 字段只声明了 `restf`、`dbf`、`bus`、`thdf` 四个。但内部 Handler 类访问了两个未声明的字段：

1. `AddComputeManagerHandler` 第 5 步调用 `evtf.fire(SdnControllerCanonicalEvents.SDN_CONTROLLER_ADDED, ...)` ——`evtf`（EventFacade）未注入，运行时 NPE。
2. `SyncResourceHandler.handleTenantRouterEvent` 调用 `znsApiClient.getTenantRouter(...)` 和 `znsApiClient.listSegments(...)` ——`znsApiClient` 未注入，运行时 NPE。

**建议**：在 `ZnsReverseNotificationFacade` 类中补充：

```java
@Autowired
private EventFacade evtf;
@Autowired
private ZnsApiClient znsApiClient;
```

---

### P0-3：`AddComputeManagerHandler` 无事务边界，多步 persist 存在数据完整性风险

**位置**：§4.2.2，步骤 2-4

**问题**：连续执行 `dbf.persist(sdnVo)` → `dbf.persist(znsVo)` → `createInherentTag(...)` 三步，中间任意一步失败（如 `ZnsControllerVO` 主键冲突或 SystemTag 写入异常）将导致 `SdnControllerVO` 孤岛记录残留在数据库中，且无回滚。重复执行通知时因 name 已存在而直接返回错误，但孤岛记录仍无法清除，数据库状态被污染。

另外，名称重复检查（check-then-act）与 persist 之间没有事务保护，并发场景下两个相同通知可能同时通过检查、同时 persist，违反业务唯一性约束。

**建议**：将步骤 2-4 包裹在同一个 `@Transactional` 方法或使用 `new SQLBatch()` 块中；同时对 `SdnControllerVO` 的 `computerManagerUuid`（通过唯一索引或 DB 约束）进行唯一性保障，而非仅凭 name 检查。

---

### P0-4：`VmNicVO.type` 赋值时机与 `KVMRealizeL2GeneveNetworkBackend.realize()` 的先后顺序未验证

**位置**：§4.7.2 `computeVmNicType`；§4.8.3 `realize()` 方法

**问题**：`realize()` 中 DPDK 分支判断依赖 `nic.getType()`：

```java
if (VmNicType.VHOSTUSER.toString().equals(nic.getType())) {
    prepareDpdkNic(hostUuid, nic, completion);
}
```

`nic.getType()` 的值来自 `VmNicManagerImpl.computeVmNicType()`。设计未说明该方法的计算结果何时被持久化到 `VmNicVO.type`，也未确认 `VmNicVO` 在进入 `KVMRealizeL2GeneveNetworkBackend.realize()` 之前 `type` 字段是否已被正确写入。若 `VmNicVO.type` 在 `realize()` 执行时仍为 `null` 或 `NONE`，DPDK socket 永远不会被创建，但 VM 会以错误配置启动，且没有任何报错。

**建议**：设计必须明确以下两点：
1. `computeVmNicType()` 在哪个执行阶段被调用并将结果写入 `VmNicVO.type`（应早于 `KVMRealizeL2GeneveNetworkBackend.realize()` 调用链）；
2. 在 `realize()` 入口加断言或日志，记录 `nic.getType()` 的当前值，以便在集成测试中可观测。

---

## P1 问题（重要，应修复）

### P1-1：Handler 内抛出 `ApiMessageInterceptionException` 语义错误

**位置**：§4.3.2 `WizardInitSyncHandler`；§4.10.1 `SyncFabricHandler`；§4.10.2 `SyncResourceHandler`

**问题**：三个 Handler 在找不到对应控制器时均抛出 `ApiMessageInterceptionException`。该异常专用于 `ApiMessageInterceptor` 的拦截阶段，在 HTTP 请求处理上下文中抛出此异常的行为未定义——可能被 Spring 全局异常处理器捕获为 500，也可能导致非预期响应，与 `NotificationResponse` 格式不一致，ZNS 无法正常解析错误。

**建议**：将错误场景改为返回 `NotificationResponse{success:false, error:"..."}` 的 JSON 字符串，或统一抛出一个约定的业务 `RuntimeException`（需配套 Spring `@ExceptionHandler` 处理）。

---

### P1-2：并发推送通知时 reconcile 操作缺乏串行化保护

**位置**：§4.10.1 `SyncFabricHandler`；§4.10.2 `SyncResourceHandler`

**问题**：两个 Handler 均以 `Completion(null)` 触发异步 reconcile 后立即返回。若 ZNS 短时间内连续推送多条通知（网络抖动重试或批量事件），同一 controller 的 `syncDeviceResourcesFromZns` / `reconcileSegmentsCloudAsSoT` 将并行执行，导致：

- 并发 upsert 同一 `ZnsTenantRouterVO`/`ZnsTransportZoneVO`：主键冲突或数据覆盖；
- 并发孤儿删除：一个线程正在使用的记录被另一个线程删除，引发 `DataIntegrityViolationException`。

**建议**：在两个 Handler 的异步触发点加入 per-controller `GlobalLock` 或使用 `ZStack ThreadFacade.syncSubmit` 排队执行，保证同一 controllerUuid 的 reconcile 操作串行化。

---

### P1-3：`needsSegmentUpdate` 对 L2 对象使用了不存在的 gateway 方法

**位置**：§4.12.2 `needsSegmentUpdate`

**问题**：

```java
private boolean needsSegmentUpdate(ZnsApiCommands.SegmentData znsSegment,
                                    L2NetworkInventory cloudL2) {
    if (!Objects.equals(znsSegment.gateway_address, cloudL2.getGatewayV4())) return true;
    if (!Objects.equals(znsSegment.gateway6_address, cloudL2.getGatewayV6())) return true;
    if (znsSegment.mtu != cloudL2.getMtu()) return true;
    ...
}
```

`L2NetworkInventory` 不存在 `getGatewayV4()`、`getGatewayV6()`、`getMtu()` 方法——网关是 L3/IpRange 层概念，MTU 也通常存储在 L3 或独立配置，而非 L2 Inventory。此代码无法通过编译。即便手动扩展 L2Inventory，将 L3 信息下推到 L2 层也是架构倒置。

**建议**：`needsSegmentUpdate` 应接受 `L3NetworkInventory`（或 `IpRangeInventory` 列表），从 L3/IpRange 层获取 gateway 和 MTU 信息进行比较。方法签名应改为：

```java
private boolean needsSegmentUpdate(ZnsApiCommands.SegmentData znsSegment,
                                    L3NetworkInventory cloudL3)
```

---

### P1-4：`AddComputeManagerHandler` 重复检查仅基于 name，遗漏 computerManagerUuid 唯一性

**位置**：§4.2.2，步骤 1

**问题**：当前重复判断逻辑：

```java
boolean nameExists = Q.New(SdnControllerVO.class)
    .eq(SdnControllerVO_.name, cmd.name)
    .isExists();
```

只检查名称是否重复。若 ZNS 因网络超时重试推送相同通知（`computerManagerUuid` 相同、`name` 相同），第二次请求仍会被视为"重复而拒绝"。但若用户后续重命名控制器，再次推送原始通知将绕过检查创建重复记录。真正的业务唯一键是 `computerManagerUuid`。

**建议**：将重复检查改为基于 `computerManagerUuid` 的 SystemTag 查询（调用 `findControllerByComputerManagerUuid(cmd.computerManagerUuid)`），若已存在则直接返回现有 UUID（幂等语义），而非报错：

```java
SdnControllerVO existing = findControllerByComputerManagerUuid(cmd.computerManagerUuid);
if (existing != null) {
    return JSONObjectUtil.toJsonString(result(true, existing.getUuid(), null)); // 幂等
}
```

---

## P2 问题（次要）

### P2-1：`AddComputeManagerNotification` 携带明文密码

**位置**：§4.1.3 `ZnsNotificationCommands.AddComputeManagerNotification`

**问题**：通知 DTO 中包含 `password` 字段明文传输。虽文档注明 Phase 1 依赖网络隔离，但在设计层面未提及任何传输加密或证书验证措施，调试日志若打印完整 body（`ZnsCallbackController.java` 现有模式为 `logger.debug(body)`）将导致密码泄露到日志文件。

**建议**：在 `ZnsReverseNotificationFacade` 的日志打印位置对 `password` 字段做脱敏处理；并在设计中明确 Phase 2 的 HMAC/TLS 升级路径。

---

### P2-2：`cleanupOrphanSegments` 未过滤 computerManagerUuid，存在跨控制器误删风险

**位置**：§4.3.3

**问题**：`cleanupOrphanSegments` 调用 `znsApiClient.listSegments(znsIp, ...)` 获取段列表，若参数中未携带 `cms_uuid`（computerManagerUuid）过滤条件，将拉取该 ZNS 实例下所有 Segment，包括属于其他 computerManager 的 Segment。这些 Segment 在 Cloud 中没有 L2 对应（因为它们属于别的 CM），会被误判为孤儿并强制删除。

**建议**：在 `cleanupOrphanSegments` 中显式传递 `computerManagerUuid` 过滤参数，与 `initSdnController` Flow 5 中的 `segParams.put("cms_uuid", ...)` 保持一致。

---

### P2-3：`SyncFabricNotification.scope` 值集合暴露 ZNS 内部拓扑术语

**位置**：§4.1.3 `SyncFabricNotification`；§4.9.2 `shouldSync`

**问题**：`scope` 取值 `TZ | TN | HOSTSWITCH` 直接映射 ZNS 内部实体概念（TransportZone、TransportNode、HostSwitch），违反 NA-3 精神（API 参数不应暴露实现层细节）。同时 `shouldSync` 对 Tenant/TenantRouter 只响应 `"ALL"` scope，无法做精细粒度触发——若 ZNS 未来需要仅触发 Tenant 同步，现有枚举集合无法支持且需改代码。

**建议**：将 scope 值抽象为 Cloud 视角语义：`ALL | FABRIC | TENANT`；`FABRIC` 对应 TZ+TN+HOSTSWITCH，`TENANT` 对应 Tenant+TenantRouter 同步。在 `shouldSync` 中映射：

```java
private boolean shouldSync(String scope, String category) {
    if ("ALL".equalsIgnoreCase(scope)) return true;
    return category.equalsIgnoreCase(scope);
}
```

---

### P2-4：`WizardInitSyncHandler` 异步触发后立即返回 `success:true`，ZNS 无法感知同步失败

**位置**：§4.3.2 末尾返回逻辑

**问题**：FlowChain 以 `.start()` 异步启动，`handleSyncHttpCall` 立即返回 `{success:true}`，ZNS 收到成功响应但实际同步可能几秒后失败（TenantRouter 拉取失败、DB 写入失败等）。ZNS 侧无重试机制，同步静默失败后 Cloud 状态可能不完整（如缺少 TenantRouter 记录）。

**建议**：在设计文档中明确说明此为"请求已受理（202 Accepted）"语义，ZNS 应在后续操作时（如创建 VPC L3）通过 Cloud 查询 API 验证同步状态，或依赖 `reconnectSdnController` 的周期性 reconcile 作为补偿机制。

---

## 亮点（做得好的地方）

- **SoT 分组清晰**：将 `reconnectSdnController` 拆分为 Group A（ZNS-as-SoT 设备资源）和 Group B（Cloud-as-SoT Segment）逻辑分明，职责边界明确，比原来的单一 FlowChain 更易维护。
- **SystemTag 迁移有兼容窗口**：`ENABLE_DPDK_VHOSTUSER` 改为 `ZNS_NIC_MODE` 时，`resolveZnsNicMode()` 提供了 v4 fallback 读取链，避免了升级时的强制迁移。
- **孤儿 TenantRouter 告警而非静默删除**：当 ZNS 删除了 Cloud 中还有 L3 引用的 TenantRouter 时，设计选择触发 `EVT_ZNS_TENANT_ROUTER_ORPHAN_IN_CLOUD` 事件而非强删，符合 Cloud-as-SoT 的安全语义。
- **FlowChain `NoRollbackFlow` 使用合理**：在 Reconcile 类 Flow 中（幂等 upsert 操作），均使用 `NoRollbackFlow`，避免了 rollback 时重复删除或状态不一致的问题。
- **错误码体系完整**：6 个新错误码均有 i18n 映射，区分 argerr/operr 类型，与现有编码规范一致。
- **实现顺序 Wave 划分合理**：Wave 1 先建数据模型、Wave 3 建通道、Wave 4 建流程的依赖拓扑排序正确，降低了并行开发的集成风险。
