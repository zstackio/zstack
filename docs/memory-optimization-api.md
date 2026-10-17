# Memory optimization API contract

Memory optimization uses the existing GlobalConfig / ResourceConfig APIs for KSM and ZRAM settings, with dedicated APIs for configuration views, Host maintenance tasks, and Host/VM memory statistics. All paths below are relative to the standard `/zstack/v1` REST root.

This guide covers the 15 public API messages, their request and response models, Java SDK usage, and ZWatch monitoring. Use GlobalConfig / ResourceConfig for ordinary settings and the dedicated memory APIs for policy inspection, maintenance, tasks, and statistics.

A Connected KVM Host schedules an asynchronous read-only observation through the Host connection extension; periodic scanning provides a fallback. Observations are evaluated against license, policy, transaction and unresolved-operation checks. Reconnection preserves pool state, Unknown history and confirmed pauses.

After a Host reboot, the same path may restore only KSM values proven by the saved applied policy and the previous successful task's durable Agent receipt. The previous task may contain only changed fields: each must agree with the complete applied target, and its native receipt must independently prove every restored switch/scan value. Zero-page settings are restored only if that task explicitly carried them. A fresh observation and matching control identity are required; unresolved or paused control is not bypassed. The previous enabled/disabled intent is preserved. License expiry does not prevent this continuation of confirmed settings, but still rejects new or queued configuration changes. It does not replay the old task or apply pending ZRAM policy, and adds no public parameter.

Python SDK collection query values use repeated URL-encoded keys, for example `hostUuids=host-a&hostUuids=host-b`; an empty collection contributes no query parameter. Credentials remain in the existing authorization header. Invalid numeric query values return the platform's standard HTTP 400 parameter error, not an internal server error.

Backend-resource readiness accepts RFC3339 observation timestamps with `Z` or a positive/negative timezone offset and fractional seconds, including on Java 8. It compares the original instant without rewriting the observation time; the existing 90-second backend proof bound, future-time rejection, identity and capacity checks remain unchanged. This adds no API parameter.

## Frontend configuration model and pages

**Users edit memory optimization settings through the existing configuration pages. MemoryPolicy is a backend view of effective configuration and related execution metadata, not a separate policy resource for users to create, name, select or attach.** Do not add a MemoryPolicy management page, policy selector or policy JSON editor.

GlobalConfig stores Global settings and ResourceConfig stores Cluster and Host values. The four feature-intent switches combine enabled values across these levels; other parameters use the nearest configured Host > Cluster > Global value. These standard settings are the sole value source. GetMemoryPolicy supplies a read-only effective-configuration view and capability information; PreviewMemoryPolicy evaluates a proposed change without saving it. The preview payload is not a second configuration store or an alternative save operation.

| Existing page or interaction | Read/display APIs | Configuration or operation APIs |
| --- | --- | --- |
| Global settings → Memory optimization settings (全局设置 → 内存优化设置) | Existing GlobalConfig reads; GetMemoryPolicy with resourceUuid=global; optional PreviewMemoryPolicy for the proposed Global change | UpdateGlobalConfig once per setting; no Host selection or per-Host configuration writes |
| Cluster details → Configuration (集群详情 → 配置) | Existing ResourceConfig reads; GetMemoryPolicy with the Cluster UUID; optional PreviewMemoryPolicy for the proposed Cluster change | UpdateResourceConfig, UpdateResourceConfigs or DeleteResourceConfig with the Cluster UUID; no per-Host configuration writes |
| Host details → Configuration (物理机详情 → 配置) | Existing ResourceConfig reads; GetMemoryPolicy with the Host UUID; GetMemoryStates for actual state; optional PreviewMemoryPolicy | UpdateResourceConfig, UpdateResourceConfigs or DeleteResourceConfig with the Host UUID |
| Global/Cluster/Host maintenance menus and confirmation dialogs | GetMemoryStates and current policy/preview evidence for the selected Hosts | UpdateMemoryPolicy with pause, resume or drain; each Host must satisfy that action's current requirements |
| Host writeback device selection or optional explicit pool maintenance | GetHostMemoryWritebackBackends and current Host state/preparation evidence | UpdateMemoryPolicy with prepareWritebackBackend for automatic selected-device maintenance. prepareZramPool is optional explicit maintenance, not a prerequisite for ordinary capacity saves. Both target one Host. |
| Ordinary configuration feedback | GetMemoryStates; display each affected Host's `status` using the seven-value mapping below. Other fields are diagnostics; no task identifier from the save Event is required | Read-only after the standard configuration save |
| Explicit maintenance-task progress and failure-alarm task details | GetMemoryTasks; refresh GetMemoryStates to confirm actual application | CancelMemoryTask only for a queued, undispatched task |
| Home memory overview and Cluster memory summary | GetMemorySummary for current values/coverage; ZWatch for historical trends | Read-only; Cluster summaries pass the actual Host UUID set, not a clusterUuid parameter |
| Host list and Host details → Overview | GetMemoryStates for state, current per-Host savings and quality; GetMemorySummary can summarize a single Host | Read-only display; reuse existing detail/configuration links |
| Host details → Monitoring data → existing memory-reclaim chart | ZWatch metrics, directly or through the existing ZQL/BFF route | Read-only; extend the existing chart rather than adding a second savings chart |
| VM details → Memory optimization statistics | GetVmMemoryOptimization; ZWatch for applicable VM historical metrics | Read-only; no VM policy, participation, capacity or enable/disable form |
| VM list → ZRAM savings column | GetVmMemoryOptimizations for the current page's VM UUIDs | Read-only; batch the request rather than querying each VM separately |

Expose only the product settings supported at that resource level: KSM and zero-page switches, ZRAM enablement/capacity, and the writeback switch on Global, Cluster and Host. Writeback device identity, capacity and preparation are Host-only. Advanced scan/scheduling tuning remains a backend/CLI operation; the UI may display its effective values. Do not render every backend configuration key as an editable control. Desired writeback enablement can be saved before a Host backend is configured or ready; execution remains gated separately. Do not disable the ordinary writeback-enable save because no backend is selected or `writebackReady` is false. Preserve the existing configuration permission, License and value checks; read States after saving to distinguish saved intent from actual application. Saving the switch does not prepare a device, and preparing a device does not enable the switch.

The memory optimization UI saves ordinary settings through UpdateGlobalConfig, UpdateResourceConfig(s) or DeleteResourceConfig according to the resource level being edited. A Global KSM, ZRAM or writeback switch updates one GlobalConfig value; a Cluster switch updates one ResourceConfig value using the Cluster UUID. Neither workflow requires a Host list or client-side iteration over Hosts. The backend determines the affected Hosts from the resulting effective configuration and handles per-Host application. Global and Cluster changes preserve stored Host values, but may change a feature's effective intent even when the Host has stored false. Newly added Hosts use the same switch-combination and nearest-parameter rules.

This release does not add a Host batch-configuration page. Global and Cluster pages edit their own configuration, not a selected set of Host overrides. Related fields on one resource use the existing atomic UpdateResourceConfigs request; that does not create a cross-Host transaction. Invalidate previews after edits, and do not automatically retry writes whose responses are lost or unknown. Configuration values and their sources are distinct from actual Host state and unapplied reasons.

The UI does not submit UpdateMemoryPolicy.apply/clearOverride as a substitute for standard configuration APIs. Revision and control evidence needed for maintenance are obtained from API replies and handled by the client, not entered by the user as configuration fields. The backend compatibility appendix is not a frontend configuration workflow.

### Saving settings when the previous result is Unknown

A valid ordinary GlobalConfig/ResourceConfig update or deletion can be saved while a Host has an unresolved result. Do not disable ordinary Save based on a preliminary States read, and do not require the user to run reconcile, recoverUncertain or drain first. The standard API still validates permission, license, field values and the complete atomic configuration candidate. Its successful Event confirms the saved desired values, not Host execution.

After saving, the backend can queue a bounded, read-only verification of the original unfinished result using the existing task executor. It preserves the original control identity and execution revision. An already running query is not duplicated by another save. The Agent must verify an original completed receipt with the same Host, operation, revision and current boot; it must not continue an unfinished device worker, repeat a write, rewrite a historical receipt, or infer completion from matching configuration alone. The MN separately reads fresh current state. Missing, stale or contradictory proof leaves `Unknown` and the original protection in place.

During a rolling upgrade, the MN first uses the existing read-only Agent state request to verify explicit support for completed-result verification, together with the current Host/boot and sample freshness. An old Agent without that capability is not sent a new reconcile action as a probe: older versions may interpret an unknown action as ordinary reconciliation. Missing support keeps the saved desired value and `Unknown`; upgrading software alone is not proof that the original operation completed. This internal capability is not a public request parameter or a frontend prerequisite.

If that exact capability rejection is the only reason the automatic query stopped, a fresh observation of the upgraded Agent can schedule one further read-only check under the same saved request. It preserves the original operation, revision and control identity and does not repeat device work. Other unresolved results are not retried by each periodic observation. Original Global/Cluster policy operations retain their original scope and resource identity during verification; the query's Host ownership does not relabel their journals.

Only proven completion permits the existing planner to apply the **latest** saved effective configuration. A second save during verification must not be overwritten by the earlier target. An older KSM receipt from a different boot does not prove current KSM values. For a completed first-device preparation, the current-boot device receipt and fresh inactive/no-pool/backend identity prove that device operation; an older successful KSM-only control, including an explicit empty KSM policy, remains historical rather than being promoted to a current acknowledgement. A changed backend selection cannot be silently overwritten by completion of an older selection.

Ordinary pages submit the standard save once, then read States and map its existing seven-value `status`. `Unknown` remains visible when the outcome cannot be verified. Detailed task/reason/currentStep fields are for diagnostics, not an extra frontend state machine. Losing the save response does not authorize submitting a new write automatically. This behavior adds no public API, query parameter, configuration key or status enum.

Task history records what happened to each attempt. A save that could not dispatch while verification was in progress can retain a historical `Blocked` task even after a later application of the newest desired values succeeds. The current States result follows that latest application; do not derive it from an arbitrary historical task or rewrite the blocked attempt as if it executed.

### Capacity changes through ordinary configuration

Save explicit logical/physical ZRAM capacity values through the same standard configuration APIs. When the effective values differ from a qualified running pool, the backend performs the necessary maintenance as part of applying that configuration. The frontend does not submit separate drain, preparation or restore requests for this change, and a successful standard save Event does not mean that the pool has already changed.

The backend preserves the effective KSM settings and writeback intent while it performs the following sequence for each eligible Host:

1. Drain the old pool using its current boot, pool and control identity, and verify the original operation's completion and current native state.
2. Prepare the RAM-only pool, or re-prepare the same previously owned writeback backend after verifying its original ownership and fresh device identity. This does not select a replacement disk.
3. Activate the pool with the saved target capacity. Writeback remains disabled during activation.
4. Verify the new pool and actual capacity, then restore the retained writeback setting if it was enabled. A RAM-only change finishes without creating a writeback-enable operation.

These steps are Host children of the original configuration task; the backend does not report that root successful before the required steps finish. A confirmed pause, unresolved operation, stale observation, changed boot, changed effective target or conflicting device/control identity prevents continuation. A lost response remains unresolved; an explicit existing reconciliation query may continue only from a verified original result and fresh observation, without replaying the old drain or preparation. The configuration remains saved even when a Host cannot apply it yet.

For ordinary save feedback, read `GetMemoryStates` rather than depending on an API Event's `apiId`, a root-task UUID, or a new task-link API. A Global page pages through the existing States endpoint. A Cluster page first queries its KVM Hosts through the existing Host query and then requests States for that nonempty UUID set. If the Cluster has no Hosts, show an empty result locally: an empty or omitted `hostUuids` on States means all KVM Hosts, not that Cluster. Follow `nextPage` with the returned `snapshotId` and restart the read on a snapshot conflict. Display saved configuration separately from per-Host application status and actual readings; `Unknown` is not successful application or a zero measurement.

Both States routes return the same seven-value `status`. The frontend maps this one field to its label; it does not combine task, plan, reason or native-state fields to infer the configuration result.

| `status` | Display label | Meaning |
| --- | --- | --- |
| `Queued` | 等待执行 | The operation is queued. |
| `Applying` | 配置中 | Configuration is being applied. |
| `Draining` | 排空中 | The pool is being drained. |
| `Succeeded` | 配置已应用 | The applicable configuration operation succeeded; this does not promise positive memory savings or continuous compression. |
| `Failed` | 配置失败 | The configuration operation failed. |
| `Blocked` | 暂无法执行 | The latest target cannot currently proceed or requires review. |
| `Unknown` | 状态待确认 | A trustworthy outcome is unavailable; do not present this as success, failure or a zero measurement. |

The backend preserves an in-progress or unresolved control status. Without an active operation, a latest `Blocked`/`NeedsReview` plan replaces an older completed status with `Blocked`; a failed or unresolved plan likewise cannot be hidden by an old success. A terminal status still bound to an active task, a plan claiming progress without an active operation while the stored control status is terminal, and an unsupported stored status are returned as `Unknown`. This is a read-only response projection: it does not change stored tasks, release control, replay an operation, or change native-state eligibility and `allowedActions`. Metric quality remains separate from configuration status.

Each States inventory's nullable `currentStep` remains an optional backend/CLI diagnostic projection of its current bound task, not a frontend prerequisite, a new task or a native runtime measurement. Capacity maintenance uses `drain`, `prepareZramPool` or `prepareWritebackBackend`, `activateZramPool`, then `restoreWriteback` when needed. Other current actions retain their names: `apply`, `pause`, `resume`, `reconcile` and `recoverUncertain`; an ordinary `clearOverride` is displayed as `apply`. `policyPlanStatus`, `reason` and detailed `state` likewise remain diagnostics, not additional inputs to frontend status mapping. A step with `Unknown` does not prove completion. No current task, a missing/mismatched task or an obsolete terminal reference yields no step. A completed change has no current step. The field is projected during the existing paged read without changing stored configuration or task state.

Removing a capacity override with an explicit parent capacity follows that parent's value. If no level retains an explicit capacity, this release does not introduce an implicit reset/rebuild to the percentage defaults: it retains the existing unmanagement behavior and native capacity. The automatic maintenance contract above applies to changes to explicit effective capacity values.

## Data sources

| Frontend use | API/data source |
| --- | --- |
| Ordinary configuration | Existing UpdateGlobalConfig, UpdateResourceConfig(s) and DeleteResourceConfig; see the standard configuration section below |
| Effective policy, capability hints and task status | Policy reads/preview and GetMemoryTasks below; a task is not a measurement |
| Current Host savings and cloud/zone coverage | GetMemoryStates and GetMemorySummary; premium reads the node_exporter/Prometheus chain |
| Current VM savings and unavailable reasons | Single/batch VM memory APIs, using premium's Prometheus reader |
| Historical Host/VM trends | Existing ZWatch `/zwatch/metrics`, directly or via ZQL `return with (zwatch{...})` |

The 15 management entry points are distinct from the 14 monitoring metrics listed below. UI clients use the platform metric API and resource labels, not raw PromQL, an Agent URL or a Go-service URL. The existing cluster Prometheus route handles the node owning the scrape; task/state snapshots are not substitutes for historical samples.

## Public entry points and consumers

| API / Java SDK Action | Method and path | Consumer and purpose |
| --- | --- | --- |
| GetMemoryPolicy | GET `/memory-policies/{resourceUuid}` | Configuration pages: read-only effective settings, revision and capabilities. resourceUuid is the current resource UUID, or global for Global settings. This does not create a user-managed policy resource. |
| PreviewMemoryPolicy | POST `/memory-policies/{resourceUuid}/preview` | UI/CLI: read-only preview and per-Host blocked fields before configuration. Body wrapper is `params`; `policy` is a JSON string. |
| UpdateMemoryPolicy | PUT `/memory-policies/{resourceUuid}/actions` | Explicit maintenance: pause, resume, drain, selected-device prepareWritebackBackend, and optional prepareZramPool. Ordinary configuration saves use GlobalConfig / ResourceConfig and do not require frontend maintenance orchestration. Other actions are backend/CLI or compatibility workflows. Action wrapper `updateMemoryPolicy`. |
| CancelMemoryTask | PUT `/memory-optimization/tasks/{uuid}/actions` | UI/CLI: cancel a queued, not-yet-dispatched task, not deletion of task history. Wrapper `cancelMemoryTask`. `/memory-tasks/{uuid}/actions` is a compatibility alias to the same API. |
| DeleteMemoryTask | DELETE `/memory-optimization/tasks/{uuid}` | Admin/CLI: explicitly delete an eligible terminal root task and all Host children; preserves a permanent compact idempotency receipt. Never contacts Host/Agent or changes policy/revision. |
| GetMemoryStates | GET `/memory-optimization/states` | UI/CLI: current Host state, quality, revision and control-operation identity. A nonempty `hostUuids` list must contain only existing KVM Hosts; invalid targets reject the request, duplicates do not duplicate results. Omitted or empty list means all current KVM Hosts (unlike Summary's explicit empty-set semantics). Preferred over the legacy Query entry. |
| GetMemoryTasks | GET `/memory-optimization/tasks` | Optional operational task/alarm diagnostics or explicit task details. Normal configuration and selected-device progress use States `status` only. Do not add a task-history list to Host details. |
| GetHostMemoryOperations | GET `/hosts/{hostUuid}/memory-optimization/operations` | Backend/CLI diagnostics only: query the Host-local operation journal; not the management-node task table. |
| GetMemorySummary | GET `/memory-summary` | UI/CLI: Host memory-savings summary and coverage; optional `hostUuids`/`zoneUuid`. Omitted Host list means all KVM Hosts; explicit empty list selects none; duplicates are deduplicated. Standard HostVO validation rejects nonexistent/wrong-resource UUIDs; non-KVM Hosts are rejected before the Zone intersection. A valid empty intersection is allowed. Metrics use the node_exporter/Prometheus chain. |
| GetVmMemoryOptimization | GET `/vm-instances/{vmUuid}/memory-optimization` | VM detail/CLI: read-only VM statistics with resource permission and current instance identity checks. |
| GetVmMemoryOptimizations | GET `/vm-instances/memory-optimizations` | VM list/CLI: batched statistics for explicit `vmUuids`; not one Agent call per VM. |
| GetHostMemoryWritebackBackends | GET `/hosts/{hostUuid}/memory-writeback-backends` | UI/CLI: read-only writeback-device discovery and preparation state; arbitrary paths are not authorization to use a device. |
| QueryMemoryState (deprecated) | GET `/memory-states` | Existing clients only; same state response and explicit parameters as GetMemoryStates. |
| QueryMemoryTask (deprecated) | GET `/memory-tasks` | Existing clients only; same task response and explicit parameters as GetMemoryTasks. |
| QueryHostMemoryOperations (deprecated) | GET `/hosts/{hostUuid}/memory-operations` | Existing backend/CLI clients only; same response as GetHostMemoryOperations. |

Java SDK Action class names have the `Action` suffix; API message names have `API`/`Msg`. GetMemoryStates, GetMemoryTasks and GetHostMemoryOperations accept the parameters listed below, including inherited fields. Their SDK Actions return `QueryMemoryStateResult`, `QueryMemoryTaskResult` and `QueryHostMemoryOperationsResult`, respectively, because the platform generates Result names from the shared Reply class.

Infrastructure/policy/task/Host-diagnostic APIs are SystemAdmin-only. The two VM statistic APIs use platform VM read permission and resource/account checks, rather than bypassing IAM or requiring SystemAdmin for every VM read. Query permission is not configuration permission; Cloud License gates configuration, not read-only statistics or existing data readback.

VM requests enter the standard platform authorization chain, which selects the backend for the session (including IAM2 project VirtualIDs) and checks API/RBAC permission. The memory service does not re-run legacy UserVO policy evaluation. It additionally uses the platform account-visible VM set for ownership, public and directed sharing checks; IAM2 sessions use their project account. Every VM in a batch must be accessible, otherwise the entire request is denied. An authorized VM without a monitoring sample returns unavailable statistics, not a permission error. Internal callers must use the normal API entry; a fabricated session sent directly to the service is not an alternate authenticated interface.

### Resource identity

GetMemoryPolicy, PreviewMemoryPolicy and UpdateMemoryPolicy calls identify the resource through `resourceUuid`: the server resolves Host/Cluster/VM from the platform ResourceVO registry; only the literal `global` selects Global. Missing, unsupported and non-KVM Host/Cluster resources fail instead of silently falling back. Update resolves the identity before Host-only maintenance checks and idempotency hashing. The frontend uses the resource UUID from the current page; no resource-type selector is required.

## Explicit task-history cleanup

MemoryTask history has no automatic age-based TTL, matching the existing LongJob policy. `DeleteMemoryTask` is the explicit cleanup path and accepts only a root task UUID. It atomically removes the root and its full Host-child set, bumps the task-query snapshot version, and preserves a permanent five-field tombstone (`taskUuid`, `requestKey`, `requestHash`, `originalStatus`, `deletedDate`) so a previously accepted `clientRequestUuid` can never be replayed as a new operation. The tombstone has no TTL.

Deletion is rejected for queued/running/Unknown/Blocked roots or children, inconsistent partial aggregates, open child sets, target-snapshot replay evidence, active/control/recovery/migration references, malformed Host control-state JSON, or undelivered failure notifications. A missing or already deleted UUID is a successful no-op with `deleted=false`; a child UUID is rejected and callers must use its root. Successful deletion only removes task history and delivered outbox bookkeeping; platform audit/alarm history remains governed by platform policy. This API is admin-only and performs no Host/Agent operation, policy mutation, revision change or License-gated configuration.

```http
DELETE /zstack/v1/memory-optimization/tasks/<rootTaskUuid>
Authorization: OAuth <sessionUuid>
```

When the root request is replayed with the same actor, `clientRequestUuid`, and request hash, submit returns `MEMORY_REQUEST_RESULT_PURGED` with the original task UUID/status; using the same UUID with a different hash remains `MEMORY_IDEMPOTENCY_CONFLICT`. The delete API itself is idempotent and does not accept a new client request UUID.

## Read parameters and pagination

| Read API and compatibility alias | Business parameters | Response |
| --- | --- | --- |
| GetMemoryStates / QueryMemoryState | `hostUuids` (repeat query key for a list), `start`, `limit`, `snapshotId` | `inventories`, `total`, `nextPage`, `snapshotId` |
| GetMemoryTasks / QueryMemoryTask | `uuid`, `hostUuid`, `status`, `start`, `limit`, `snapshotId` | `inventories`, `total`, `nextPage`, `snapshotId` |
| GetHostMemoryOperations / QueryHostMemoryOperations | path `hostUuid`; query `operationId`, `vmUuid`, `status`, `start`, `limit` | Host operation `inventory` (the existing local-journal response contract) |

- These are explicit read APIs, not AutoQuery APIs. None accepts generic `q`, `conditions`, `fields`, `sortBy`, `sortDirection`, `count`, `replyWithCount` or undeclared dotted/collection parameters. All six read routes enforce strict REST parameter checking and reject unsupported names with HTTP 400. This strict parameter validation applies to the listed read routes.
- `start` is an offset. `limit` follows `memoryOptimization.query.defaultPageSize` (100) and `query.maxPageSize` (500); those are configurable page budgets, not Host/VM count caps.
- For state/task pagination, use the returned snapshot token for subsequent pages. A changed query version is a conflict: restart from the first page instead of silently combining pages from different snapshots. Task state transitions can invalidate a token even when the number of rows is unchanged.
- Use the Get Actions for read requests. The deprecated Query routes are compatibility aliases with the same explicit parameters; they do not support generic AutoQuery filters.
- SDK `MemoryTaskInventory` includes `expectedControlOperationUuid` and `reconcileOperationUuid`; `MemoryPolicyInventory` includes `migrationExclusion`. `Unknown` remains unresolved, not success.
- `reconcile` queries the original operation; it never reapplies its policy. A verified ordinary Host success retains that original control ID and applies the original task's delta/clear semantics to MN bookkeeping. If an older MN already completed the query but retained its query ID, the same existing `reconcile` action can query that successful original again, with unchanged desired revision. Automatic/configuration dispatch remains fenced until that bookkeeping is confirmed. Host, boot, revision, current control and fresh Agent proof must still agree. A missing/contradictory proof stays unresolved; no manual database edit, new apply, or relaxed identity check is required.
- An ordinary successful reconciliation may return an immutable historical receipt. MN then uses the existing read-only Agent `state` endpoint to obtain independent current proof; it does not add a current timestamp or identity to the old receipt. The state envelope identifies the locally verified Host and boot, current sample time and same-boot confirmed original operation/revision. Both replies and the persisted active/control/revision must agree. A missing/expired read, a reboot between reads, or a changed control owner keeps the query unresolved. This is internal readback, not another user operation or a configuration replay; dedicated maintenance/VM recovery contracts remain unchanged.
- Sparse backend/tuning updates also read the native CPU policy. If the original operation temporarily disabled dispatch, tuning equality alone cannot complete it: its final CPU receipt and current revision/values must agree. A missing final receipt remains `Unknown`; a lost final GET can be reconciled from the original successful receipt and matching current readback. Reconciliation never issues the missing CPU write or silently reenables dispatch.
- A capacity-only update while dispatch is already off does not issue a CPU-policy write. Its reconciliation instead requires the original capacity request/receipt, matching capacity CAS and CPU readback revisions, unchanged CPU values, and the original tuning evidence. A verified `NATIVE_POOL_UNQUALIFIED` result is `Blocked`, not successful native activation; missing evidence or intervening writes remain `Unknown`. This uses the existing read-only reconciliation API and does not initialize a pool or toggle dispatch.

## Standard configuration APIs

The 46 ordinary scalar settings use **GlobalConfig / ResourceConfig as the sole value source**. An ordinary configuration request contains only the standard configuration API parameters; no memory-specific resource-type, revision, execution-target or request-ID fields are added. The existing API message identity and authenticated actor are used internally; never invent these extra fields for the standard API. The 15 specialized memory entry points above are not 15 replacements for standard configuration APIs.

| Existing SDK Action | Method and path | Use |
| --- | --- | --- |
| UpdateGlobalConfig | PUT `/global-configurations/{category}/{name}/actions` | Update one Global setting from UI or CLI; wrapper `updateGlobalConfig`; no resource UUID or Host list |
| UpdateResourceConfig | PUT `/resource-configurations/{category}/{name}/{resourceUuid}/actions` | Update one setting on the Host or Cluster identified by resourceUuid; wrapper `updateResourceConfig` |
| UpdateResourceConfigs | POST `/resource-configurations/{resourceUuid}/resource-configs/actions` | Atomically update multiple settings on one Host or Cluster; resourceUuid is that resource's UUID, not a Host list; wrapper `params`, list `resourceConfigs` |
| DeleteResourceConfig | DELETE `/resource-configurations/{category}/{name}/{resourceUuid}` | Remove this resource's explicit value so the normal parent value applies; no JSON body |

Category is `kvm`. The KSM switch is `host.ksm`. The remaining keys use `memory.` followed by the policy field path: for example `memory.ksm.zeroPagesEnabled`, `memory.ksm.pagesToScan`, `memory.ksm.sleepMillis`, `memory.zram.enabled`, `memory.zram.logicalCapacityBytes` and `memory.zram.ramLimitBytes`. The exact mapping and permitted resource levels are defined by [MemoryStandardField](../plugin/kvm/src/main/java/org/zstack/kvm/memory/MemoryStandardField.java). `memory.writeback.enabled` is valid on Global, Cluster and Host; `memory.writeback.backendResourceUuid` and `memory.writeback.backendCapacityBytes` are Host-only. Global and Cluster requests cannot bind a Host device or prepare it.

Writeback intent and Host backend configuration are independently saved. Either may be configured first; disabling writeback preserves the backend, and saving/preparing a backend does not enable writeback or ZRAM. Legal desired values remain saved when the Host lacks a backend, a ready backend or runtime support. The existing `policyPlanStatus`, `policyBlockedFields`, task reasons and actual-state reads describe why execution has not occurred; API save success is not Applied. A Host can execute only when inherited writeback intent, ZRAM intent, fresh backend identity/readiness, capability and existing safety checks all permit it. The existing configuration/Host reconciliation path re-evaluates after preparation or capability changes without asking the user to toggle the parent switch again. No additional public API, polling loop or configuration store is introduced.

The four feature-intent switches (`ksm.enabled`, `ksm.zeroPagesEnabled`, `zram.enabled` and `writeback.enabled`) combine enabled values across Host, Cluster and Global. A Host `true` enables the desired feature; otherwise an enabled Cluster or Global value enables it. A local `false` is stored as `false`, but does not disable an enabled parent. With boolean values at all three levels, the desired feature is disabled only when all three are `false`. The legacy KSM `none` boundary described below remains unmanaged, not a boolean or an empty value. Capability, licensing, pause/drain, unresolved operations and other safety checks still independently govern actual execution.

Other parameters use the nearest configured value: Host, then Cluster, then Global. For example, logical capacities of 4 GiB, 8 GiB and 16 GiB respectively produce 4 GiB; deleting the Host value selects 8 GiB, and deleting the Cluster value then selects 16 GiB. The three Host CPU/Memory PSI/I/O PSI guard settings follow this parameter rule, not the feature-switch rule. A valid numeric `0` remains an explicit value. Host-only backend settings do not inherit from Cluster or Global.

The read-only `MemoryPolicyInventory.policy` contains this scope's values, while `effectivePolicy` reports the combined desired result. These are distinct from standard ResourceConfig queries that may resolve a value through their normal inheritance lookup. The Host editor must distinguish a local switch set to `false` from an effective feature enabled by an upper level. For enabled feature switches, `fieldSources` identifies the nearest level contributing `true`; when all applicable values are `false`, it retains the nearest explicitly configured source. Remove a custom value with the standard per-field DELETE operation, not an empty value or `none`. Removing a value is not pool deletion, swapoff, drain, journal cleanup or permission to clear an unresolved operation.

Scalar prevalidation is read-only. The actual commit validates the complete proposed configuration and affected inherited Host policies, then writes configuration and the memory revision/task records in the same transaction. A rejected configuration or rolled-back write commits none of them and sends no successful configuration notification. A batch validates the combined values as one configuration. Notifications are emitted after commit. Non-memory settings retain their standard configuration behavior.

New-cloud defaults are created by the existing GlobalConfig initialization path: KSM, zero-page merging and ZRAM desired switches are enabled; writeback is disabled. Upgrades preserve existing switch values (including `host.ksm=none` and an empty unmanaged zero-page setting), even on an old cloud with no Hosts. Unset scan settings use 2000 pages and 20 milliseconds; explicit Global/Cluster/Host values are retained. There is no fresh-cloud installer argument, initialization API, separate bootstrap resource or default-seeding polling loop. Desired defaults still require ordinary per-Host capability and safety validation before execution.

KSM is controlled through the distribution's native `ksm.service` and `ksmtuned.service`, without an additional scan daemon. If the kernel has no `use_zero_pages` interface, ordinary KSM remains supported and internal completion evidence marks zero pages as unsupported; state does not invent `zeroPagesEnabled=false`. A request explicitly enabling zero pages is rejected before writes. Permission errors, malformed values and missing required ordinary-KSM evidence remain errors. Existing complete zero-page readback evidence is still verified strictly; this compatibility change adds no public parameter or API.

When both the saved KSM target and actual scanning are disabled, existing Host policy reconciliation also checks explicitly managed scan/zero-page settings against fresh, same-boot readback anchored to the confirmed control operation. It submits only a proven difference through the existing capability and task-admission checks, retaining the effective scan parameters without starting KSM. Matching values produce no repeat task; missing, stale or unconfirmed observations and unresolved operations cannot authorize correction. Paused lifecycle protection remains in effect. This adds no API, configuration value or polling task, and is not a cross-boot restore mechanism.

Legacy `host.ksm` has three explicit values: `true`, `false`, and `none`. An explicit `none` stops inheritance of **that switch**, leaves it unmanaged, and is not an alias for `false` or a deleted row. Deleting the ResourceConfig override reveals the parent. The read-only `MemoryPolicyInventory.fieldModes` reports `{"ksm.enabled":"Unmanaged"}` for an explicit unmanaged source; `effectivePolicy` and the Agent target omit that switch. Other KSM tuning/zero-page fields remain independent. When a tuning-only target has no switch, Agent preserves the existing valid setting or reads native `run=0/1` before its first write; missing/invalid native state rejects the change instead of implicitly enabling KSM. VM participation policies have no KSM field mode.

Blank override values are rejected. A corrupt blank persisted resource row is not silently treated as inheritance; explicit deletion repairs it. Upgrade migration preserves conflicting standard/legacy values and reports the conflict, including explicit `none`; it does not overwrite either side. Schema defaults are not reported as explicit resource overrides. Ordinary nonparticipant ResourceConfig extensions retain synchronous invocation and exception propagation. Local canonical ResourceConfig update/delete events are emitted only after the database transaction commits; a rolled-back write emits no event, and a received remote event is not rebroadcast. Atomic opt-in mutations continue to defer their participant notifications until commit.

A valid desired setting can be saved while an affected Host is disconnected, lacks a fresh observation, is paused or has an unresolved operation. The API success then means **desired configuration was saved**, not that Host execution succeeded. Per-Host reconciliation records expose the blocked reason; the existing operation identity and Unknown/pause/drain protections remain unchanged. Saving a new setting cannot release an old operation's fence. The frontend displays each Host's outcome from States `status` only. Effective policy and GetMemoryTasks remain optional operational diagnostics, not additional frontend result checks.

Global configuration supports single-field updates. For dependent capacity changes on one Host/Cluster, use the existing ResourceConfig batch API. Global single-field changes must each leave a valid complete configuration; two separate HTTP requests are not atomic.

Examples (all UUIDs and values are illustrative; use normal platform authentication):

Enable the Global KSM default. Global and Cluster settings apply through inheritance and do not overwrite more specific values:

```http
PUT /zstack/v1/global-configurations/kvm/host.ksm/actions
Content-Type: application/json

{"updateGlobalConfig":{"value":"true"}}
```

Enable the Global ZRAM default with a separate standard configuration request. For either switch, use the string value "false" to disable it at the same resource level:

```http
PUT /zstack/v1/global-configurations/kvm/memory.zram.enabled/actions
Content-Type: application/json

{"updateGlobalConfig":{"value":"true"}}
```

Set one Cluster's KSM switch to false, preserving stored Host values. An enabled Global or Host switch still enables the effective intent. The client sends one request with the Cluster UUID:

```http
PUT /zstack/v1/resource-configurations/kvm/host.ksm/<clusterUuid>/actions
Content-Type: application/json

{"updateResourceConfig":{"value":"false"}}
```

Use memory.zram.enabled in the same Cluster route to configure its ZRAM switch. Deleting that Cluster's override restores inheritance from Global:

```http
DELETE /zstack/v1/resource-configurations/kvm/host.ksm/<clusterUuid>
```

Save a disabled KSM switch at the Host level:

```http
PUT /zstack/v1/resource-configurations/kvm/host.ksm/<hostUuid>/actions
Content-Type: application/json

{"updateResourceConfig":{"value":"false"}}
```

If either the Cluster or Global KSM switch is `true`, the Host's effective desired switch remains enabled. To remove the stored Host value instead, use DELETE:

```http
DELETE /zstack/v1/resource-configurations/kvm/host.ksm/<hostUuid>
```

For the Host ZRAM switch, replace `host.ksm` with `memory.zram.enabled` in these two routes. Parent switch changes can change the effective result without overwriting the stored Host `false`. Neither request clears independent zero-page, scan, capacity or writeback-backend settings; removing those values is a separate per-field operation subject to existing validation and Host-only backend rules. Display application outcome from the current Host States `status`; effective policy and task details are optional diagnostics. Ordinary configuration feedback does not depend on obtaining a task identifier from the standard Event.

Enable zero-page handling for one Host:

```http
PUT /zstack/v1/resource-configurations/kvm/memory.ksm.zeroPagesEnabled/<hostUuid>/actions
Content-Type: application/json

{"updateResourceConfig":{"value":"true"}}
```

Update coupled ZRAM capacity fields atomically for one Host. For Cluster ZRAM capacity settings, use the Cluster UUID in the same route; Host writeback-backend capacity is not a Cluster setting:

```http
POST /zstack/v1/resource-configurations/<hostUuid>/resource-configs/actions
Content-Type: application/json

{"params":{"resourceConfigs":[
  {"category":"kvm","name":"memory.zram.logicalCapacityBytes","value":"34359738368"},
  {"category":"kvm","name":"memory.zram.ramLimitBytes","value":"17179869184"}
]}}
```

Remove one Host's zero-page override so its parent setting applies:

```http
DELETE /zstack/v1/resource-configurations/kvm/memory.ksm.zeroPagesEnabled/<hostUuid>
```

Save a Global writeback intent even before individual Hosts have backends:

```http
PUT /zstack/v1/global-configurations/kvm/memory.writeback.enabled/actions
Content-Type: application/json

{"updateGlobalConfig":{"value":"true"}}
```

Store `false` for one Cluster, then remove that local value. An enabled Global or Host writeback switch still enables the effective intent; neither request deletes any Host backend:

```http
PUT /zstack/v1/resource-configurations/kvm/memory.writeback.enabled/<clusterUuid>/actions
Content-Type: application/json

{"updateResourceConfig":{"value":"false"}}
```

```http
DELETE /zstack/v1/resource-configurations/kvm/memory.writeback.enabled/<clusterUuid>
```

For backend/CLI configuration of an **already prepared** resource, save one Host's valid backend fields without including an enable switch. Obtain its identity/capacity from the existing Host candidate API; these sample placeholders are not permission to use an arbitrary disk. A frontend selecting a new candidate uses the automatic preparation flow below instead; it does not call this batch API after preparation:

```http
POST /zstack/v1/resource-configurations/<hostUuid>/resource-configs/actions
Content-Type: application/json

{"params":{"resourceConfigs":[
  {"category":"kvm","name":"memory.writeback.backendResourceUuid","value":"<hostBackendResourceUuid>"},
  {"category":"kvm","name":"memory.writeback.backendCapacityBytes","value":"21474836480"}
]}}
```

Preparation still uses the existing explicit Host maintenance workflow and fresh proof described below. Saving fields does not prepare, format or replace an active device. Inspect the inherited desired value and actual application independently:

```http
GET /zstack/v1/memory-policies/<hostUuid>
```

```http
GET /zstack/v1/memory-optimization/states?hostUuids=<hostUuid>
```

The same standard `UpdateGlobalConfigAction`, `UpdateResourceConfigAction`, `UpdateResourceConfigsAction` and `DeleteResourceConfigAction` handle these examples; no new SDK Action is required. Unknown operations, stale evidence, confirmed pauses and device ownership protections are unchanged.

Values above are strings because that is the existing configuration API contract; capacities are exact decimal bytes, not GiB strings or floating-point numbers. Follow the usual asynchronous API event/job flow, then query effective policy/state for Host application. Standard configuration APIs return their platform Event schemas.

Use the standard configuration SDK Actions for scalar settings on Global, Cluster and Host pages, and use resource batch updates for coupled fields. Read-only policy, capability and monitoring APIs provide the corresponding display data. Do not save ordinary settings through the specialized policy action endpoint. Backend compatibility behavior is described separately in the appendix.

## Configuration and maintenance boundary

- Basic configuration uses the standard APIs above, including KSM/zero pages, ZRAM capacity and prepared per-Host writeback-device selection. Optional policy preview is read-only and does not replace commit-time validation. The four feature switches combine enabled intent; other parameters use Host > Cluster > Global precedence. Standard and compatibility APIs use the same stored values and effective rules.
- UI/CLI controlled operations include pause/drain/resume and per-Host pool/backend preparation within the corresponding workflow. Backend/CLI only: `reconcile`, `recoverUncertain`, compatibility `clearOverride`, target-shard staging/commit/cancel, VM participation/exclusion and advanced tuning. Device preparation is not exposed as an arbitrary shell or reset operation.
- `expectedRevision`, source revisions, request identity and required control/recovery evidence remain mandatory where applicable to the specialized UpdateMemoryPolicy workflow, not added to standard configuration API parameters. Preparation and recovery never waive License, ownership, running-operation or data-safety checks.
- `allowedActions` is a current-state hint, not a durable authorization or promise that a later request cannot conflict. `preflightRequiredActions` identifies actions needing additional evidence; it must not be rendered as unconditional permission. Submission performs the final checks.
- `recoverUncertain` preserves the original unresolved history and only releases the operation after verified recovery evidence. It is not offered as a generic UI retry button.

## Preview warnings

`warningDetails` provides `code`, localized `message`, `messageKey`, and `formatArgs` for machine-readable warning handling. The `warnings` string list provides text messages for compatible clients. Warning creation does not increment the platform error counter.

| Stable code | Message key |
| --- | --- |
| VM_EXCLUSION_GLOBAL_SWAP | `memory.preview.warning.vmExclusionGlobalSwap` |
| HOST_REVALIDATED_BEFORE_APPLY | `memory.preview.warning.hostRevalidatedBeforeApply` |
| CLOUD_LICENSE_REQUIRED | `memory.preview.warning.cloudLicenseRequired` |
| ZRAM_CAPACITY_UNVERIFIED | `memory.preview.warning.zramCapacityUnverified` |

Do not parse English warning text to make capability decisions. A warning is not an error response; configuration rejection still uses platform error metadata and preserves stable MEMORY_* reason codes.

## GET, POST, PUT and DELETE examples

Use existing session authentication (`Authorization: OAuth <sessionUuid>`); POST/PUT use `Content-Type: application/json`. The examples contain placeholders, not runnable credentials. GET and DELETE have no JSON body or `params` wrapper. Lists use repeated or indexed keys, not a JSON-array string or a comma-separated value.

```http
GET /zstack/v1/memory-policies/<hostUuid>
GET /zstack/v1/memory-optimization/states?hostUuids.0=<hostUuid>&start=0&limit=100
GET /zstack/v1/memory-optimization/tasks?uuid=<taskUuid>
DELETE /zstack/v1/memory-optimization/tasks/<rootTaskUuid>
GET /zstack/v1/vm-instances/memory-optimizations?vmUuids.0=<vmUuid1>&vmUuids.1=<vmUuid2>
```

POST `/memory-policies/<hostUuid>/preview` is a synchronous read-only preview. The preview request body uses the `params` wrapper:

```json
{"params":{"action":"apply","policy":"{\"ksm\":{\"enabled\":true}}"}}
```

Read `inventory`, `hostResults`, `warnings` and `warningDetails`. Preview supports `targetHostUuids` and `clearOverrideFields` when applicable. The `apply` value in this **read-only preview** describes the proposed configuration; it does not authorize a policy-action write. Confirm and save the edited fields through the standard configuration APIs above, then display each affected Host's States `status`. Effective policy and related tasks are optional operational diagnostics.

For explicit maintenance, PUT `/memory-policies/<hostUuid>/actions` uses the `updateMemoryPolicy` wrapper with the selected action and its required evidence. Actions are `pause`, `resume`, `drain`, `prepareWritebackBackend` and `prepareZramPool`. Explicit pool preparation is an optional maintenance/diagnostic workflow, not a prerequisite or frontend step for ordinary capacity saves. The complete action-specific requirements are defined in [APIUpdateMemoryPolicyMsg](../plugin/kvm/src/main/java/org/zstack/kvm/memory/APIUpdateMemoryPolicyMsg.java) and the action examples below. Backend recovery, VM participation and target-shard actions are not ordinary configuration-page operations.

For Host writeback device selection, submit `prepareWritebackBackend` once with the selected candidate and `backendPreparation.resetConfirmed=true`. `backendCapacityBytes` is the candidate's measured **whole-device size**, not an arbitrary quota or partition size. The backend validates the fresh candidate and current control identity, drains when necessary, prepares the selected device, and records its verified resource UUID and capacity through the existing Host ResourceConfig adapter before restoring the retained policy. The frontend must not invent a resource UUID, issue a second configuration PUT, or orchestrate drain/prepare/activation. Existing KSM, ZRAM and writeback switch intent is retained; preparation alone does not authorize enabling a previously disabled feature. Display progress and outcome solely through States `status` as described above.

Candidate `canPrepare` describes whether **direct offline preparation** is currently possible. A false value due only to `MEMORY_SERVICE_NOT_STOPPED` and/or `DRAIN_PROOF_UNAVAILABLE` may still permit the automatic request when all existing drain and identity checks pass. Missing eligibility, empty or other rejection reasons, unsafe candidates, unresolved operations and insufficient drain capacity do not permit bypassing those checks. The backend rechecks the candidate after draining and requires the normal offline preparation proof before touching the selected device. Loss of a trustworthy completion result remains `Unknown`; querying States does not replay maintenance.

For first use, a verified `poolGeneration=none` with only `MEMORY_SERVICE_NOT_STOPPED` does not require a drain of a nonexistent pool. The same preparation request verifies that there is no pool, data or unresolved operation, records its intent, normally exits the verified service process, prepares the device and restores the service. It does not force-kill on timeout, stop VMs or change KSM settings. Uncertain exit or changed identities stop further preparation and retain the existing operation protection; a repeated request or a States query does not replay the exit. No extra frontend request or public parameter is required.

A previously prepared backend does not itself constitute a ZRAM pool. Selecting another unused device before pool creation uses the same single request, without first enabling ZRAM. The backend must bind the completed preparation evidence to the current backend ownership and configuration, independently verify current control and physical absence of a pool or data, and preserve the old backend evidence through the existing maintenance archive before claiming the new device. An `APPLIED` label, a missing generation, or an old KSM acknowledgment alone is not sufficient. Incomplete, conflicting or unresolved evidence still blocks preparation; neither the client nor an operator should delete the old records to make it pass.

The browser requires the existing `memory.writeback.prepare` permission and the selected-device/whole-disk reset confirmation, not a separate `memory.optimization.drain` permission. A legacy BFF payload may still require `drainConfirmed` for schema compatibility; it must not trigger a second drain request or provide authority to bypass backend checks. The user selects a candidate and confirms the destructive device preparation; UUIDs, measured capacity, boot/pool identity and revisions come from current API data, not manually entered proof fields.

Preparation remains an admitted asynchronous operation, not unconditional success on receipt. Unsafe or unverifiable admission returns the standard error without dispatching device work. Once admitted, display the actual result through States; a successful REST job or task creation is not completed preparation. The BFF evidence reads described below are still required to form this maintenance request; this is distinct from using States as a hard prerequisite for an ordinary configuration save.

Candidate `writebackReady=true` proves device readiness only, not that the management-node binding or the complete maintenance task succeeded. Do not use that flag alone to return successful completion. A no-write already-configured result requires fresh, same-Host identity, successful current States with no active operation and matching desired/applied revisions, and matching effective/applied/actual backend UUID and capacity. If those proofs are missing, the ready device is unbound, or the result remains Unknown, return an explicit error without reformatting, replaying preparation or issuing an extra configuration request. Ordinary configuration saving retains the safe completion behavior described above.

The client obtains the applicable revision, complete source-revision chain and control/boot/pool evidence from current API replies. For `prepareWritebackBackend`, take `expectedControlOperationUuid` from the same Host's States `controlOperationUuid`: this is the management-node control identity. The identically named field in `GetHostMemoryWritebackBackends.inventory` is an Agent observation and must not substitute for it. Take `expectedRevision` and the complete `expectedSourceRevisions` from Policy; take the candidate identity, measured capacity, boot and pool generation from Backends. Require the same-Host States inventory before submitting. When that inventory has no control UUID (null or omitted), preserve the existing cold-first-use contract by omitting `expectedControlOperationUuid`; do not fabricate a control or use the Agent identity instead. A missing inventory, a different Host or a failed read is not evidence of no control. Preserve `clientRequestUuid` when retrying the identical maintenance request; never construct a partial source map or treat a revision conflict as success. These maintenance fields are not added to GlobalConfig / ResourceConfig requests or exposed as user input fields.

Cancel a queued task with PUT `/memory-optimization/tasks/<taskUuid>/actions` and body `{"cancelMemoryTask":{}}`; `/memory-tasks/<taskUuid>/actions` is a compatibility alias with identical authorization and cancellation rules. Task-history DELETE is an administrator backend/CLI operation, not a configuration-page control.

Raw PUT uses the existing async REST protocol: HTTP 202 accepts only the REST job and supplies a `location` and `apiTimeout`; poll that original location with the normal SDK/BFF job handling. Only a successful final API reply containing `MemoryTask` inventory confirms that the maintenance task was accepted. Its `inventory.uuid` identifies that memory task, not the REST job. Normal configuration and selected-device preparation results are displayed through States `status`; the browser does not need a separate task-history workflow. If the existing BFF action waits for maintenance completion, it must wait for the exact returned root task's aggregate result, not just the successful prepare child: binding or policy-restoration siblings may still be queued. Use the existing GetMemoryTasks UUID filter and pagination without a Host filter that excludes the root; require the original root UUID, Host scope/resource and action to match, and preserve snapshot and error handling. Neither HTTP 202 nor API completion proves Host execution success. A final job error must remain an error, not be reported as a queued memory task. Use the standard error object and MEMORY_* code, not a presumed HTTP 409; invalid HTTP input can be 400 and platform failed replies/events can be 503.

State/task `nextPage` is the next offset, not page number. Carry filters, limit and `snapshotId`; a null nextPage ends pagination. `MEMORY_QUERY_SNAPSHOT_CHANGED` requires restarting at offset zero without the old snapshot token. This is result-set consistency, not freezing measurement timestamps. Strict reads reject undeclared filters; ZQL metric integration does not add generic AutoQuery semantics to these APIs.

### Host list status filtering through existing ZQL

The Host list keeps the existing Host ZQL query, ordering, paging and `return with (total)`. `MemoryState` is an inventory mapping to the existing state table, not a new resource, API or SQL view. Select `MemoryState.hostUuid` in a subquery to restrict Hosts **before** paging/counting. Infrastructure memory-state reads remain administrator-only, including direct ZQL and nested subqueries; ordinary Host visibility checks remain in place.

The page sends the selected public `states.status` values. BFF translates them to predicates on the persisted `MemoryState.status`, `activeTaskUuid` and `policyPlanStatus`; it must not fetch all States/UUIDs to filter a Host page in memory. Raw ZQL `status` is the stored control result and is not always the final States presentation. The translation is:

| Public status | Predicate meaning |
| --- | --- |
| `Queued`, `Applying`, `Draining` | The raw status equals the selected value, regardless of plan/activity. |
| `Succeeded` | Raw `Succeeded`, no active task, and no plan overriding the result. |
| `Failed` | Raw `Succeeded`/`Failed`/`Blocked`, no active task, and plan `Failed`; or raw `Failed`, no active task, and no overriding plan. |
| `Blocked` | Raw `Blocked` with an active task; or any of the three terminal raw statuses with no active task and plan `Blocked`/`NeedsReview`; or raw `Blocked` with no active task and no overriding plan. |
| `Unknown` | Every remaining Host, **including Hosts without a MemoryState row**. Implement as the complement of the six known public statuses, not only `raw status='Unknown'`. |

An overriding plan is `Blocked`, `NeedsReview`, `Failed`, `Unknown`, `Queued`, `Applying` or `Draining`. A null/other plan retains the terminal raw result when there is no active task. An active raw `Succeeded`/`Failed` is `Unknown`; this does not alter stored state, execution ownership or recovery rules. Multi-select uses the union of selected predicates; negation excludes that union. `featureState` and metric `quality` are different fields and must not substitute for this status filter.

For example, pass the following query through the existing authenticated `GET /zstack/v1/zql` `zql` parameter, URL-encoded by the normal client:

```sql
query Host.uuid,name where uuid in
  (query MemoryState.hostUuid where status='Succeeded' and activeTaskUuid is null
   and (policyPlanStatus is null or policyPlanStatus not in
        ('Blocked','NeedsReview','Failed','Unknown','Queued','Applying','Draining')))
return with (total) order by uuid asc limit 20 offset 0
```

After the Host page is returned, use the existing States request with **only that page's Host UUIDs** to populate the status column; an absent state displays `Unknown`. Do not filter this returned page again or recompute its total from page-local data. A later observation may change status between the two reads; no cross-API snapshot is promised. Failed/missing observations must not be presented as a successful applied state or fabricated zero savings.

The VM batch reply is an `inventories` map keyed by every distinct requested VM UUID. At the 60-second overall deadline, all unfinished VMs—including already-issued Host queries and Hosts not yet started—receive `quality=unavailable`, `reason=BATCH_DEADLINE_EXCEEDED`, `metrics=null`. Completed results and no-current-Host reasons are retained. A late callback neither changes the returned map nor emits a second reply. HTTP success must not be interpreted as complete metric coverage.

### Update action parameter boundaries

Every action resolves and validates the real resource before any task or shard write. Parameters belonging only to another action are rejected, not silently ignored. `expectedControlOperationUuid`, when supplied for an applicable action, is part of its idempotency identity; changing it while reusing a request UUID is a conflict.

| Parameter group | Applicable actions and constraints |
| --- | --- |
| `clearOverrideFields` | `clearOverride` only |
| `backendPreparation`, `poolPreparation`, `recovery` | Only their corresponding prepare/recover action; never mixed |
| `expectedInstanceGeneration` | VM resource actions only; running VM participation changes require the current instance |
| `targetSnapshotGeneration`, `targetShardCount`, `targetTotalCount`, `targetDigest` | `stageTargetShard` and `commitTargetShards` only |
| `targetShardIndex`, `targetVmUuids` | `stageTargetShard` only; commit consumes persisted shards |
| `expectedSourceRevisions`, `expectedGlobalRevision` | Rejected for stage/cancel; checked when supplied for other actions. Supplying both requires both to match |
| `expectedControlOperationUuid` | Rejected for all three shard actions; `prepareWritebackBackend` uses the same Host's States/MN control identity, not the Backends/Agent observation; other actions validate their current or recovery-specific control fence |
| `targetHostUuids` | Explicit Global/Cluster execution targets; forbidden for Host/VM execution and shard cancellation. For legacy staging only, accepted as a VM-shard alias when `targetVmUuids` is absent |
| `policy` | Configuration only for apply/commit; safety/preparation/recovery and stage/cancel require no configuration |

Use `targetVmUuids` for staging; never send both target-list fields. Staging binds `expectedRevision` to the snapshot, commit checks the current revision, and cancellation must match the staged revision. Shard actions operate on Global/Cluster/Host policy resources, not VM resources. Deleted or mismatched resources are rejected for all actions, including shard cancellation.

## Typed maintenance and observation models

`recovery`, `backendPreparation` and `poolPreparation` are fixed `MemoryUncertainRecovery`, `MemoryBackendPreparation` and `MemoryZramPoolPreparation` objects. Unknown/duplicate members and wrong JSON types are rejected using the original request JSON before generic Map conversion can hide them. Capacity must be an exact in-range integer number; an equivalent JSON literal such as `8192.0` or `8.192e3` remains accepted, while a string, fraction or overflow is rejected. Omit inapplicable objects; do not send a null object or mix maintenance object types in one action.

All twelve action bodies and their preconditions are published in [the action examples](../plugin/kvm/src/main/resources/memory-optimization/update-memory-policy-actions.json). Each `request` is wrapped as `{"updateMemoryPolicy": REQUEST}` and sent to PUT `/zstack/v1/memory-policies/RESOURCE_UUID/actions`, using the same resource UUID in the body and path and normal OAuth authentication. Use current read/preview values for revision, complete source chain, boot/pool/control fences and a new request UUID; the illustrative UUIDs are not live targets. `apply`/`clearOverride` are compatibility operations; ordinary configuration uses the standard APIs above.

`recoverUncertain` is backend/CLI-only and supports two narrow cases, not arbitrary Unknown operations:

- Maintenance activation retirement requires the exact successful `drainControlOperationUuid` as before.
- A tuning-only Host apply that committed tuning but did not restore its temporarily disabled CPU reclaim gate omits `drainControlOperationUuid` (not `null`). The Agent verifies the original tuning/guard receipts, unchanged native revisions/settings, the same Host/boot/pool/control, and no active operations. It issues only the original final CPU CAS request and saves its receipt. It never replays tuning, compression, writeback or drain.

For both cases, the original task remains Unknown for audit and replay protection. The new recovery task may finish only with verified evidence. CPU recovery returns `phase=POLICY_RECOVERED`, the original applied revision, and the new recovery control UUID; MN merges only the original confirmed policy into the applied snapshot. Pre-dispatch rejection restores the original unresolved slot. If the CPU PUT response is lost before its receipt is durable, recovery stays Unknown even when a later GET shows equal values. Reconciliation of this completed recovery receipt is read-only and can finish only when the saved receipt and current versions agree. Repeating a completed recovery returns the original receipt and does not issue another PUT. This is not a blanket read-only contract for every explicit maintenance reconcile: existing maintenance reconciliation can continue a proven pending step under its original controlled-continuation rules. The automatic `verify-completed` query after ordinary configuration saving is strictly read-only and never continues a worker or issues a policy/device write.

A legacy recovery child rejected with the exact persisted `RECOVERY_REQUEST_INVALID` result before Agent admission may have no local journal. The existing `reconcile` action can seal only that child as failed/not issued after matching the original parent request, Host/boot/control, submission time, pool identity and absence of running operations under the Host writer lock. The durable rejection prevents replay of that old ID. The original partially applied task stays Unknown and requires the existing controlled recovery operation; no CPU/tuning/data-plane operation is issued by this query. Missing, conflicting or unwritable evidence remains Unknown. There is no new public action or proof parameter.

VM replies use `MemoryVmAccountingInventory` (batch: map from every distinct requested VM UUID to that model). Public names use camelCase: `observedAt`, `hostBootId`, `schemaVersion`, `ownershipSemantics`, nested identity and metric names. Missing values stay null, observed zero stays zero, and valid negative savings/differences are not clamped. uint64 identity tokens remain decimal strings; unsafe already-rounded floating-point identities are unavailable. Samples use their own `observedAt` and server `displayTtlMillis`, not request time.

The response models expose seven compatibility aliases: `observed_at`, `identity.instance_generation`, metrics `ram_original_bytes`, `ram_payload_bytes`, `backend_committed_original_bytes`, and writeback maintenance `request_id` / `updated_at`. Each alias has the same value as its canonical camelCase field, including null. Use the canonical fields in client code. The management node projects the public models from Agent/Prometheus data; protocol labels and persisted journal fields are separate from the public response names.

Host operation reads use `MemoryHostOperationsInventory`: `hostUuid`, `entries`, `total`, `nextPage`, `concurrency` and optional `recordBudget`. Entries use `operationId`, `vmUuid`, `kind`, `status`, `startedAt`, optional `timeoutAt`, `completedAt` and `reason`; writeback/idle are pool-wide and may have empty `vmUuid`. `concurrency.activeOperations` is ordinary-slot occupancy (service C), including timed-out operations when isolation is full; `isolatedTimeoutOperations` is separate isolated-slot occupancy (service Q). These are counts, not configured limits. `recordBudget` exposes `actualAllocatedBytes`, `reservedBytes`, `budgetBytes`, `records` and `protectedRecords`. Missing optional budget stays absent/null, not zero. MN validates the Agent Host envelope and projects these fields; it does not forward arbitrary internal service fields. VM monitoring instead uses Prometheus and does not require an Agent envelope.

## ZWatch monitoring contract

Historical monitoring uses the standard premium ZWatch APIs:

| Method | Path | Parameters | Response |
| --- | --- | --- | --- |
| GET | `/zwatch/metrics` | Required `namespace`, `metricName`; optional `startTime`, `endTime`, `offsetAheadOfCurrentTime`, `period`, `labels`, `valueConditions`, `functions` | `data` array of `{time,value,labels}` |
| GET | `/zwatch/metrics/meta-data` | Optional `namespace`, `name` (not `metricName`) | `metrics` array with names/labels |
| GET | `/zwatch/metrics/label-values` | Required `namespace`, `metricName`, nonempty `labelNames`; optional `startTime`, `endTime`, `filterLabels` | `labels` array |

For this Prometheus path, `startTime`, `endTime`, returned point `time` and `period` use **seconds**. `offsetAheadOfCurrentTime=3600` requests the last hour and overrides the explicit range. The default range is the last minute; without `period`, ZWatch chooses a resolution. Charts should send an explicit positive integer period. Memory inventory `sampleTime`/`currentStatusSampleTime`, in contrast, use epoch **milliseconds**.

```bash
curl --get "$MN/zstack/v1/zwatch/metrics" \
  -H "Authorization: OAuth $SESSION_UUID" \
  --data-urlencode 'namespace=ZStack/Host' \
  --data-urlencode 'metricName=TotalSavedEstimateBytes' \
  --data-urlencode "labels.0=HostUuid=$HOST_UUID" \
  --data-urlencode "startTime=$START_SECONDS" \
  --data-urlencode "endTime=$END_SECONDS" \
  --data-urlencode 'period=60'
```

Illustrative response:

```json
{"data":[{"time":1791504000,"value":1048576.0,"labels":{"HostUuid":"<hostUuid>"}}]}
```

For VM history, use `namespace=ZStack/VM`, a VM metric below, and `labels.0=VMUuid=<vmUuid>`. Existing Host/VM ZQL supports `return with (zwatch{metricName='...',startTime=...,endTime=...,period=...})` on the same metric path. Preserve resource filters and missing points; do not enable gap filling that invents zero savings. ZQL/BFF wrappers can reshape labels; the response above is direct REST.

### Host metrics

Namespace `ZStack/Host` (also registered for `ZStack/KVMHost`); label `HostUuid`.

| Metric | Meaning/unit | Current summary field |
| --- | --- | --- |
| `TotalSavedEstimateBytes` | Total mechanism savings, bytes | `totalSavedEstimateBytes` |
| `KsmOrdinarySavedBytes` | Ordinary KSM savings, bytes | `ksmOrdinaryBytes` |
| `KsmZeroSavedBytes` | Zero-page KSM savings, bytes | `ksmZeroBytes` |
| `KsmTotalSavedBytes` | KSM savings total, bytes | `ksmTotalBytes` |
| `ZramSavedEstimateBytes` | ZRAM savings total, bytes | `zramBytes` |
| `ZramOrdinarySavedBytes` | Resident/non-writeback ZRAM savings, bytes | `zramNormalBytes` |
| `ZramWritebackSavedBytes` | Live writeback logical bytes, not cumulative writes | `zramWritebackBytes` |
| `MemorySampleValid` | Valid total-mechanism sample (`1` when available) | — |
| `MemorySampleEpoch` | Generation used to detect sample discontinuities | — |
| `MemorySampleTimestampSeconds` | Source sample timestamp, Unix seconds | — |

Savings use signed `mechanism-estimate-v1` accounting, not total free RAM. Display labels may omit “estimate”; API names and accounting semantics use the values shown above. Do not clamp a valid negative value to zero. Stale, ambiguous, schema-mismatched or invalid data is unavailable, not zero; display TTL defaults to 90 seconds and is configurable.

For Host charts, query the three sample-support metrics with the same range/period and align by Host UUID and time. Require MemorySampleValid=1 for the total and retain a gap when epoch changes. Components have independent quality checks: missing total does not automatically invalidate available KSM/ZRAM components. Do not double-count totals and components, or label partial Host coverage as a complete cloud total. Current summaries expose `quality`, `expectedHosts`, `coveredHosts`, `metricCoverage`, `currentStatusSampleTime`, and `currentStatusTtlMillis`.

### VM metrics

Namespace `ZStack/VM`; label `VMUuid`; all values in bytes.

| Metric | Accounting meaning |
| --- | --- |
| `ZramOriginalBytes` | Resident original bytes attributed to the source memcg |
| `ZramPayloadBytes` | Compressed payload attributed to the source memcg |
| `ZramBackendOriginalBytes` | Original bytes on writeback backend attributed to the source memcg |
| `ZramSavedBytes` | Source-memcg savings accounting value |

These are source-memcg inventories, not per-VM net physical savings or a fair allocation of allocator overhead. Do not add VM values to Host totals. Identity/instance/pool/freshness checks can make samples unavailable. Use current VM read APIs for quality/reason; empty history does not prove zero usage or that optimization is disabled.

Implementation references in premium: the ZWatch API message classes, `HostAbstractNamespace`, `VmAbstractNamespace`, `MemoryMetricRouting`, `MemoryMetricExpressions` and `MemoryPrometheusMetrics`.

## Integration through the frontend APIs

Frontend implementation and environment functional tests use the same public management-node interfaces listed in the page table. A Java SDK or BFF wrapper is suitable when it sends the actual documented REST method, route, body wrapper and parameters through the platform authorization chain.

- Exercise ordinary configuration, inheritance, deletion, atomic batches, validation failures and permission/license checks through GlobalConfig / ResourceConfig. Calling UpdateMemoryPolicy.apply/clearOverride, a Repository/Manager method, SQL or a Host-local control endpoint does not verify this frontend workflow.
- Verify Global switches with UpdateGlobalConfig and Cluster switches with the Cluster UUID through ResourceConfig, without client-side Host configuration loops. Cover the eight boolean combinations for each feature switch, same-level replacement, stored false versus effective true, nearest-parameter values including zero, and newly added Hosts. Changes to Global/Cluster require an isolated test scope; do not change protected Hosts or unresolved operations to create that isolation.
- Read effective settings and current Host application state through GetMemoryPolicy and GetMemoryStates. Exercise Global pagination and Cluster Host selection, including the empty-Cluster case. GetMemoryTasks is for explicit task progress and alarm diagnostics, not a prerequisite for ordinary save feedback. Distinguish configuration saved, execution pending/blocked and Host state actually applied.
- Exercise the exposed maintenance actions through UpdateMemoryPolicy, backend discovery through GetHostMemoryWritebackBackends, and queued-task cancellation through CancelMemoryTask. Use real replies for the action's identity and control evidence.
- Read VM statistics through the single/batch public VM APIs with the applicable account or IAM2 project session. Verify access denial and unavailable samples separately.
- Verify KSM, zero-page, ZRAM and writeback statistics through the frontend's current-statistics APIs and ZWatch historical-metric path. Native/Agent/Go/Prometheus observations can corroborate results, but do not replace the public API checks.
- Record the tested package identity, sanitized request/response, task result, Host readback and monitoring evidence. Keep unit tests, simulated dependencies, backend-only maintenance and compatibility regressions separate from environment results for the frontend path.

## Standard event alarms and UI visibility

Use the existing ZWatch event-subscription APIs, not metric alarms or a memory-specific alarm API. Both events are in `ZStack/Host`: `HostMemoryOptimizationTaskFailed` (actual failed tasks, with the existing default subscription) and `HostMemoryOptimizationServiceAbnormal` (explicit component failure). Neither event is a time-series metric.

For example, POST `/zwatch/events/subscriptions` through the standard authenticated REST root:

```json
{
  "params": {
    "name": "Host memory service failure",
    "namespace": "ZStack/Host",
    "eventName": "HostMemoryOptimizationServiceAbnormal",
    "emergencyLevel": "Normal",
    "labels": [{"key": "HostUuid", "value": "<hostUuid>"}]
  }
}
```

Select notification actions through the existing event-alarm workflow; this example alone does not configure an external email/SMS destination. Query, update, enable/disable and delete use standard EventSubscription APIs and their authorization. Do not merge subscriptions into metric Alarm pagination or create a second task-failure default subscription.

Service facts come from the existing read-only Host observation. Native KSM/ksmtuned `failed` is abnormal only while the effective policy expects it to run; `active` is the corresponding component's recovery fact. Off, paused, maintenance, disconnected, unknown, normal pressure-controlled `run=0`, and a missing optional Go service in a KSM-only deployment are not failures. Writeback requires a new native backend I/O error; recovery requires successful post-backoff I/O in the same pool, not merely an elapsed timer or successful idle marking. Existing ZWatch barriers deduplicate by Host and component. These internal observations do not add a public health API, configuration or polling task and do not override Unknown control fences.

Only administrators see memory-optimization/savings widgets, columns and configuration entries. Tenant views also stop their associated display queries. Existing single/batch VM read APIs retain their resource/account/IAM2 permission semantics; UI hiding does not make these APIs admin-only.

## Backend compatibility appendix

This section documents backend/client compatibility; it does not define an additional settings page or a frontend save path. GetMemoryPolicy, PreviewMemoryPolicy and UpdateMemoryPolicy still accept an optional `scope` parameter for existing callers. It must match the resource identified by `resourceUuid`; a conflict is rejected. Frontend calls use only the page's resource identity. Standard GlobalConfig / ResourceConfig APIs do not define this parameter. Internal policy/task metadata retains the resource level independently of the frontend request.

The compatibility `apply`/`clearOverride` actions use the same configuration values and atomic revision/task transaction, with their source-revision and target-selection checks. Frontend configuration saves and their primary functional tests use the standard configuration APIs instead. The following is a **backend compatibility request**, not the UI configuration-save example. PUT `/memory-policies/<hostUuid>/actions` uses the standard action wrapper:

```json
{
  "updateMemoryPolicy": {
    "action": "apply",
    "expectedRevision": 7,
    "expectedSourceRevisions": {"Global:global":3,"Cluster:<clusterUuid>":4,"Host:<hostUuid>":7},
    "clientRequestUuid": "<newRequestUuid>",
    "policy": "{\"ksm\":{\"enabled\":true}}"
  }
}
```

Revision values in this compatibility example are illustrative. Copy the current revision and complete source-revision map from read/preview results, and preserve the request UUID only for an identical logical request.
