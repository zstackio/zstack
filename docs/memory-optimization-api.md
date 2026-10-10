# Memory optimization API contract

Memory optimization uses the existing GlobalConfig / ResourceConfig APIs for KSM and ZRAM settings, with dedicated APIs for configuration views, Host maintenance tasks, and Host/VM memory statistics. All paths below are relative to the standard `/zstack/v1` REST root.

This guide covers the 15 public API messages, their request and response models, Java SDK usage, and ZWatch monitoring. Use GlobalConfig / ResourceConfig for ordinary settings and the dedicated memory APIs for policy inspection, maintenance, tasks, and statistics.

A Connected KVM Host schedules an asynchronous read-only observation through the Host connection extension; periodic scanning provides a fallback. Observations are evaluated against license, policy, transaction and unresolved-operation checks. Reconnection preserves pool state, Unknown history and confirmed pauses.

## Frontend configuration model and pages

**Users edit memory optimization settings through the existing configuration pages. MemoryPolicy is a backend view of effective configuration and related execution metadata, not a separate policy resource for users to create, name, select or attach.** Do not add a MemoryPolicy management page, policy selector or policy JSON editor.

GlobalConfig stores Global settings. ResourceConfig stores Cluster and Host overrides, with per-field precedence Host > Cluster > Global. These standard settings are the sole value source. GetMemoryPolicy supplies a read-only effective-configuration view and capability information; PreviewMemoryPolicy evaluates a proposed change without saving it. The preview payload is not a second configuration store or an alternative save operation.

| Existing page or interaction | Read/display APIs | Configuration or operation APIs |
| --- | --- | --- |
| Global settings → Basic settings → Hardware resources → Host (全局设置 → 基本设置 → 硬件资源 → 物理机) | Existing GlobalConfig reads; GetMemoryPolicy with resourceUuid=global; PreviewMemoryPolicy before confirmation | UpdateGlobalConfig for one setting at a time |
| Cluster details → Configuration (集群详情 → 配置) | Existing ResourceConfig reads; GetMemoryPolicy with the Cluster UUID; PreviewMemoryPolicy before confirmation | UpdateResourceConfig, UpdateResourceConfigs, DeleteResourceConfig |
| Host details → Configuration (物理机详情 → 配置) | Existing ResourceConfig reads; GetMemoryPolicy with the Host UUID; GetMemoryStates for actual state; PreviewMemoryPolicy before confirmation | UpdateResourceConfig, UpdateResourceConfigs, DeleteResourceConfig |
| Global/Cluster/Host maintenance menus and confirmation dialogs | GetMemoryStates and current policy/preview evidence for the selected Hosts | UpdateMemoryPolicy with pause, resume or drain; each Host must satisfy that action's current requirements |
| Host ZRAM pool or writeback-backend preparation dialog | GetHostMemoryWritebackBackends and current Host state/preparation evidence | UpdateMemoryPolicy with prepareWritebackBackend or prepareZramPool; device/pool preparation targets one Host |
| Operation feedback, task progress and failure-alarm task details | GetMemoryTasks; refresh GetMemoryStates to confirm actual application | CancelMemoryTask only for a queued, undispatched task |
| Home memory overview and Cluster memory summary | GetMemorySummary for current values/coverage; ZWatch for historical trends | Read-only; Cluster summaries pass the actual Host UUID set, not a clusterUuid parameter |
| Host list and Host details → Overview | GetMemoryStates for state, current per-Host savings and quality; GetMemorySummary can summarize a single Host | Read-only display; reuse existing detail/configuration links |
| Host details → Monitoring data → existing memory-reclaim chart | ZWatch metrics, directly or through the existing ZQL/BFF route | Read-only; extend the existing chart rather than adding a second savings chart |
| VM details → Memory optimization statistics | GetVmMemoryOptimization; ZWatch for applicable VM historical metrics | Read-only; no VM policy, participation, capacity or enable/disable form |
| VM list → ZRAM savings column | GetVmMemoryOptimizations for the current page's VM UUIDs | Read-only; batch the request rather than querying each VM separately |

Expose only the product settings supported at that resource level: KSM and zero-page switches, ZRAM enablement/capacity, and permitted per-Host writeback settings. Advanced scan/scheduling tuning remains a backend/CLI operation; the UI may display its effective values. Do not render every backend configuration key as an editable control. Writeback device binding/preparation is Host-specific, and Global writeback enablement is rejected.

The UI saves ordinary settings only through UpdateGlobalConfig, UpdateResourceConfig(s), or DeleteResourceConfig. It does not submit UpdateMemoryPolicy.apply/clearOverride as a substitute. Resource identity comes from the current page. Revision and control evidence needed for maintenance are obtained from API replies and handled by the client, not entered by the user as configuration fields. The backend compatibility appendix is not a frontend configuration workflow.

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
| UpdateMemoryPolicy | PUT `/memory-policies/{resourceUuid}/actions` | UI maintenance only: pause, resume, drain, prepareWritebackBackend and prepareZramPool. Ordinary configuration saves use GlobalConfig / ResourceConfig. Other actions are backend/CLI or compatibility workflows. Action wrapper `updateMemoryPolicy`. |
| CancelMemoryTask | PUT `/memory-optimization/tasks/{uuid}/actions` | UI/CLI: cancel a queued, not-yet-dispatched task, not deletion of task history. Wrapper `cancelMemoryTask`. `/memory-tasks/{uuid}/actions` is a compatibility alias to the same API. |
| DeleteMemoryTask | DELETE `/memory-optimization/tasks/{uuid}` | Admin/CLI: explicitly delete an eligible terminal root task and all Host children; preserves a permanent compact idempotency receipt. Never contacts Host/Agent or changes policy/revision. |
| GetMemoryStates | GET `/memory-optimization/states` | UI/CLI: current Host state, quality, revision and control-operation identity. A nonempty `hostUuids` list must contain only existing KVM Hosts; invalid targets reject the request, duplicates do not duplicate results. Omitted or empty list means all current KVM Hosts (unlike Summary's explicit empty-set semantics). Preferred over the legacy Query entry. |
| GetMemoryTasks | GET `/memory-optimization/tasks` | UI/CLI: follow the submitted configuration/maintenance task or inspect an alarm. Do not add a task-history list to Host details. |
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

Deletion is rejected for queued/running/Unknown/Blocked roots or children, inconsistent partial aggregates, open child sets, target-snapshot replay evidence, active/control/recovery/migration/bootstrap references, malformed Host control-state JSON, or undelivered failure notifications. A missing or already deleted UUID is a successful no-op with `deleted=false`; a child UUID is rejected and callers must use its root. Successful deletion only removes task history and delivered outbox bookkeeping; platform audit/alarm history remains governed by platform policy. This API is admin-only and performs no Host/Agent operation, policy mutation, revision change or License-gated configuration.

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

## Standard configuration APIs

The 46 ordinary scalar settings use **GlobalConfig / ResourceConfig as the sole value source**. An ordinary configuration request contains only the standard configuration API parameters; no memory-specific resource-type, revision, execution-target or request-ID fields are added. The existing API message identity and authenticated actor are used internally; never invent these extra fields for the standard API. The 15 specialized memory entry points above are not 15 replacements for standard configuration APIs.

| Existing SDK Action | Method and path | Use |
| --- | --- | --- |
| UpdateGlobalConfig | PUT `/global-configurations/{category}/{name}/actions` | Update one Global setting; wrapper `updateGlobalConfig` |
| UpdateResourceConfig | PUT `/resource-configurations/{category}/{name}/{resourceUuid}/actions` | Update one Host/Cluster setting; wrapper `updateResourceConfig` |
| UpdateResourceConfigs | POST `/resource-configurations/{resourceUuid}/resource-configs/actions` | Atomically update multiple settings on the same Host or Cluster; wrapper `params`, list `resourceConfigs` |
| DeleteResourceConfig | DELETE `/resource-configurations/{category}/{name}/{resourceUuid}` | Remove this resource's explicit value so the normal parent value applies; no JSON body |

Category is `kvm`. The KSM switch is `host.ksm`. The remaining keys use `memory.` followed by the policy field path: for example `memory.ksm.zeroPagesEnabled`, `memory.ksm.pagesToScan`, `memory.ksm.sleepMillis`, `memory.zram.enabled`, `memory.zram.logicalCapacityBytes` and `memory.zram.ramLimitBytes`. The exact mapping and permitted resource levels are defined by [MemoryStandardField](../plugin/kvm/src/main/java/org/zstack/kvm/memory/MemoryStandardField.java); the backend device UUID is Host-only. A Global writeback enable is rejected; an individual Host backend must be prepared and validated before activation.

Host overrides Cluster, which overrides Global, independently for each setting. An explicit `false` is a real override, not missing/unset. Deleting a Host value reveals the Cluster/Global value; deleting a Cluster value reveals Global while leaving Host overrides unchanged. Removing an override is not pool deletion, swapoff, drain, journal cleanup or permission to clear an unresolved operation. The UI should use its existing configuration editing/deletion conventions, without adding a separate inheritance or resource-type selection form.

Scalar prevalidation is read-only. The actual commit validates the complete proposed configuration and affected inherited Host policies, then writes configuration and the memory revision/task records in the same transaction. A rejected configuration or rolled-back write commits none of them and sends no successful configuration notification. A batch validates the combined values as one configuration. Notifications are emitted after commit. Non-memory settings retain their standard configuration behavior.

Legacy `host.ksm` has three explicit values: `true`, `false`, and `none`. An explicit `none` stops inheritance of **that switch**, leaves it unmanaged, and is not an alias for `false` or a deleted row. Deleting the ResourceConfig override reveals the parent. The read-only `MemoryPolicyInventory.fieldModes` reports `{"ksm.enabled":"Unmanaged"}` for an explicit unmanaged source; `effectivePolicy` and the Agent target omit that switch. Other KSM tuning/zero-page fields remain independent. When a tuning-only target has no switch, Agent preserves the existing valid setting or reads native `run=0/1` before its first write; missing/invalid native state rejects the change instead of implicitly enabling KSM. VM participation policies have no KSM field mode.

Blank override values are rejected. A corrupt blank persisted resource row is not silently treated as inheritance; explicit deletion repairs it. Upgrade migration preserves conflicting standard/legacy values and reports the conflict, including explicit `none`; it does not overwrite either side. Schema defaults are not reported as explicit resource overrides. Ordinary nonparticipant ResourceConfig callbacks keep the platform's synchronous behavior and exception propagation. Post-commit notification applies to atomic opt-in mutations (including every item in a mixed atomic batch), not to all unrelated settings.

A valid desired setting can be saved while an affected Host is disconnected, lacks a fresh observation, is paused or has an unresolved operation. The API success then means **desired configuration was saved**, not that Host execution succeeded. Per-Host reconciliation records expose the blocked reason; the existing operation identity and Unknown/pause/drain protections remain unchanged. Saving a new setting cannot release an old operation's fence. Read effective state and GetMemoryTasks to distinguish desired, blocked and applied results.

Global configuration supports single-field updates. For dependent capacity changes on one Host/Cluster, use the existing ResourceConfig batch API. Global single-field changes must each leave a valid complete configuration; two separate HTTP requests are not atomic.

Examples (all UUIDs and values are illustrative; use normal platform authentication):

```http
PUT /zstack/v1/global-configurations/kvm/host.ksm/actions
Content-Type: application/json

{"updateGlobalConfig":{"value":"true"}}
```

```http
PUT /zstack/v1/resource-configurations/kvm/memory.ksm.zeroPagesEnabled/<hostUuid>/actions
Content-Type: application/json

{"updateResourceConfig":{"value":"true"}}
```

```http
POST /zstack/v1/resource-configurations/<hostUuid>/resource-configs/actions
Content-Type: application/json

{"params":{"resourceConfigs":[
  {"category":"kvm","name":"memory.zram.logicalCapacityBytes","value":"34359738368"},
  {"category":"kvm","name":"memory.zram.ramLimitBytes","value":"17179869184"}
]}}
```

```http
DELETE /zstack/v1/resource-configurations/kvm/memory.ksm.zeroPagesEnabled/<hostUuid>
```

Values above are strings because that is the existing configuration API contract; capacities are exact decimal bytes, not GiB strings or floating-point numbers. Follow the usual asynchronous API event/job flow, then query effective policy/state for Host application. Standard configuration APIs return their platform Event schemas.

Use the standard configuration SDK Actions for scalar settings on Global, Cluster and Host pages, and use resource batch updates for coupled fields. Read-only policy, capability and monitoring APIs provide the corresponding display data. Do not save ordinary settings through the specialized policy action endpoint. Backend compatibility behavior is described separately in the appendix.

## Configuration and maintenance boundary

- Basic configuration uses the standard APIs above, including KSM/zero pages, ZRAM capacity and prepared per-Host writeback-device selection. Optional policy preview is read-only and does not replace commit-time validation. Host > Cluster > Global uses the same standard value source across standard and compatibility APIs.
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

Read `inventory`, `hostResults`, `warnings` and `warningDetails`. Preview supports `targetHostUuids` and `clearOverrideFields` when applicable. The `apply` value in this **read-only preview** describes the proposed configuration; it does not authorize a policy-action write. Confirm and save the edited fields through the standard configuration APIs above, then read actual Host state and related tasks.

For explicit maintenance, PUT `/memory-policies/<hostUuid>/actions` uses the `updateMemoryPolicy` wrapper with the selected action and its required evidence. UI actions are `pause`, `resume`, `drain`, `prepareWritebackBackend` and `prepareZramPool`. The complete action-specific requirements are defined in [APIUpdateMemoryPolicyMsg](../plugin/kvm/src/main/java/org/zstack/kvm/memory/APIUpdateMemoryPolicyMsg.java) and the action examples below. Backend recovery, VM participation and target-shard actions are not ordinary configuration-page operations.

The client obtains the applicable revision, complete source-revision chain and control/boot/pool evidence from current API replies. Preserve `clientRequestUuid` when retrying the identical maintenance request; never construct a partial source map or treat a revision conflict as success. These maintenance fields are not added to GlobalConfig / ResourceConfig requests or exposed as user input fields.

Cancel a queued task with PUT `/memory-optimization/tasks/<taskUuid>/actions` and body `{"cancelMemoryTask":{}}`; `/memory-tasks/<taskUuid>/actions` is a compatibility alias with identical authorization and cancellation rules. Task-history DELETE is an administrator backend/CLI operation, not a configuration-page control.

Raw PUT uses the existing async REST protocol: HTTP 202 supplies a `location` and `apiTimeout`; poll that location with the normal SDK/BFF job handling. The eventual API result's `inventory.uuid` identifies a memory task, not the REST job. Follow GetMemoryTasks for execution status/reason. API completion is not Host execution success. Use the standard error object and MEMORY_* code, not a presumed HTTP 409; invalid HTTP input can be 400 and platform failed replies/events can be 503.

State/task `nextPage` is the next offset, not page number. Carry filters, limit and `snapshotId`; a null nextPage ends pagination. `MEMORY_QUERY_SNAPSHOT_CHANGED` requires restarting at offset zero without the old snapshot token. This is result-set consistency, not freezing measurement timestamps. Strict reads reject undeclared filters; ZQL metric integration does not add generic AutoQuery semantics to these APIs.

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
| `expectedControlOperationUuid` | Rejected for all three shard actions; other actions validate their current or recovery-specific control fence |
| `targetHostUuids` | Explicit Global/Cluster execution targets; forbidden for Host/VM execution and shard cancellation. For legacy staging only, accepted as a VM-shard alias when `targetVmUuids` is absent |
| `policy` | Configuration only for apply/commit; safety/preparation/recovery and stage/cancel require no configuration |

Use `targetVmUuids` for staging; never send both target-list fields. Staging binds `expectedRevision` to the snapshot, commit checks the current revision, and cancellation must match the staged revision. Shard actions operate on Global/Cluster/Host policy resources, not VM resources. Deleted or mismatched resources are rejected for all actions, including shard cancellation.

## Typed maintenance and observation models

`recovery`, `backendPreparation` and `poolPreparation` are fixed `MemoryUncertainRecovery`, `MemoryBackendPreparation` and `MemoryZramPoolPreparation` objects. Unknown/duplicate members and wrong JSON types are rejected using the original request JSON before generic Map conversion can hide them. Capacity must be an exact in-range integer number; an equivalent JSON literal such as `8192.0` or `8.192e3` remains accepted, while a string, fraction or overflow is rejected. Omit inapplicable objects; do not send a null object or mix maintenance object types in one action.

All twelve action bodies and their preconditions are published in [the action examples](../plugin/kvm/src/main/resources/memory-optimization/update-memory-policy-actions.json). Each `request` is wrapped as `{"updateMemoryPolicy": REQUEST}` and sent to PUT `/zstack/v1/memory-policies/RESOURCE_UUID/actions`, using the same resource UUID in the body and path and normal OAuth authentication. Use current read/preview values for revision, complete source chain, boot/pool/control fences and a new request UUID; the illustrative UUIDs are not live targets. `apply`/`clearOverride` are compatibility operations; ordinary configuration uses the standard APIs above. `recoverUncertain` only covers the existing maintenance-activation retirement contract, not arbitrary ordinary Unknown operations.

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
- Read effective settings, current Host state and execution tasks through GetMemoryPolicy, GetMemoryStates and GetMemoryTasks. Distinguish configuration saved, execution pending/blocked and Host state actually applied.
- Exercise the exposed maintenance actions through UpdateMemoryPolicy, backend discovery through GetHostMemoryWritebackBackends, and queued-task cancellation through CancelMemoryTask. Use real replies for the action's identity and control evidence.
- Read VM statistics through the single/batch public VM APIs with the applicable account or IAM2 project session. Verify access denial and unavailable samples separately.
- Verify KSM, zero-page, ZRAM and writeback statistics through the frontend's current-statistics APIs and ZWatch historical-metric path. Native/Agent/Go/Prometheus observations can corroborate results, but do not replace the public API checks.
- Record the tested package identity, sanitized request/response, task result, Host readback and monitoring evidence. Keep unit tests, simulated dependencies, backend-only maintenance and compatibility regressions separate from environment results for the frontend path.

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
