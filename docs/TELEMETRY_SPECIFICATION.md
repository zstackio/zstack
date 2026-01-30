# ZStack Telemetry Specification

Version: 1.0.0
Last Updated: 2026-01-24

## Overview

This specification defines the distributed tracing standards for ZStack and its agents (Python, Go, etc.). All implementations MUST follow this specification to ensure trace context propagation works correctly across the entire system.

---

## 1. Context Propagation

### 1.1 W3C Trace Context Standard

All components MUST use [W3C Trace Context](https://www.w3.org/TR/trace-context/) for context propagation.

#### HTTP Headers

| Header | Required | Description |
|--------|----------|-------------|
| `traceparent` | YES | Contains trace-id, parent-id, and trace-flags |
| `tracestate` | NO | Vendor-specific trace data |

#### traceparent Format

```
traceparent: {version}-{trace-id}-{parent-id}-{trace-flags}
```

Example:
```
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

| Field | Length | Description |
|-------|--------|-------------|
| version | 2 chars | Always "00" |
| trace-id | 32 chars | Hex-encoded 128-bit trace ID |
| parent-id | 16 chars | Hex-encoded 64-bit span ID |
| trace-flags | 2 chars | "00" = not sampled, "01" = sampled |

#### tracestate Format

```
tracestate: key1=value1,key2=value2
```

### 1.2 Message Context (Internal CloudBus)

For internal message passing (CloudBus), trace context is stored in:

| Location | Key | Value |
|----------|-----|-------|
| ThreadContext | `traceparent` | W3C traceparent string |
| ThreadContext | `tracestate` | W3C tracestate string |
| Message Headers | `__trace__` | Map with traceparent/tracestate |

---

## 2. Span Structure

### 2.1 Span Kinds

| SpanKind | Use Case |
|----------|----------|
| `SERVER` | Receiving HTTP requests (API entry points) |
| `CLIENT` | Making HTTP requests (to agents) |
| `INTERNAL` | Internal operations (ChainTask, SyncTask, @Traced methods) |
| `PRODUCER` | Sending async messages (CloudBus.send) |
| `CONSUMER` | Receiving async messages (Message handlers) |

### 2.2 Required Spans

#### 2.2.1 API Entry (SERVER span)

**When**: HTTP request received at REST API endpoint

**Span Name**: `API {HTTP_METHOD} {PATH}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `http.method` | string | "POST" |
| `http.url` | string | "http://localhost:8080/v1/vm-instances" |
| `http.path` | string | "/v1/vm-instances" |
| `http.status_code` | int | 200 |
| `http.client_ip` | string | "192.168.1.100" |
| `net.peer.ip` | string | "192.168.1.100" |

**Context Extraction**: Extract `traceparent`/`tracestate` from HTTP headers.

#### 2.2.2 HTTP Client (CLIENT span)

**When**: Making HTTP request to agent

**Span Name**: `HTTP {METHOD}` or `HTTP {METHOD} {TARGET_SERVICE}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `http.method` | string | "POST" |
| `http.url` | string | "http://agent:7070/host/connect" |
| `http.host` | string | "agent" |
| `http.path` | string | "/host/connect" |
| `http.status_code` | int | 200 |
| `net.peer.name` | string | "agent" |
| `net.peer.port` | int | 7070 |

**Context Injection**: Inject `traceparent`/`tracestate` into HTTP headers.

#### 2.2.3 ChainTask (INTERNAL span)

**When**: Executing ChainTask in DispatchQueue

**Span Name**: `ChainTask: {TASK_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `chain.signature` | string | "vm-start-queue" |
| `chain.task.name` | string | "StartVmInstance" |
| `chain.task.class` | string | "org.zstack.compute.vm.VmStartTask" |

#### 2.2.4 FlowChain (INTERNAL span)

**When**: Executing a FlowChain (workflow orchestration)

**Span Name**: `FlowChain: {CHAIN_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `flowchain.id` | string | "FCID_a1b2c3d4" |
| `flowchain.name` | string | "CreateVmInstance" |
| `flowchain.flow_count` | int | 8 |

**Child Spans**:
- Each Flow in the chain creates a child INTERNAL span
- Rollback operations create separate spans with `flow.rollback=true`

#### 2.2.5 Flow (INTERNAL span)

**When**: Executing individual Flow within a FlowChain

**Span Name**: `Flow: {FLOW_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `flow.name` | string | "AllocateHostFlow" |
| `flow.class` | string | "org.zstack.compute.allocator.AllocateHostFlow" |
| `flow.index` | int | 3 |
| `flowchain.id` | string | "FCID_a1b2c3d4" |

#### 2.2.6 Rollback Flow (INTERNAL span)

**When**: Rolling back a Flow after failure

**Span Name**: `Rollback: {FLOW_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `flow.name` | string | "AllocateHostFlow" |
| `flow.class` | string | "org.zstack.compute.allocator.AllocateHostFlow" |
| `flow.rollback` | boolean | true |
| `flowchain.id` | string | "FCID_a1b2c3d4" |

#### 2.2.7 SyncTask (INTERNAL span)

**When**: Executing SyncTask in DispatchQueue

**Span Name**: `SyncTask: {TASK_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `sync.signature` | string | "vm-state-sync" |
| `sync.task.name` | string | "SyncVmState" |
| `sync.task.class` | string | "org.zstack.compute.vm.VmStateSyncTask" |
| `sync.level` | int | 5 |

#### 2.2.8 Task (INTERNAL span)

**When**: Executing general Task via ThreadFacade.submit()

**Span Name**: `Task: {TASK_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `task.name` | string | "AsyncHttpCallback" |
| `task.class` | string | "org.zstack.core.rest.HttpCallbackTask" |

#### 2.2.9 PeriodicTask (INTERNAL span)

**When**: Executing scheduled periodic task

**Span Name**: `PeriodicTask: {TASK_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `periodic.task.name` | string | "HeartbeatCheck" |
| `periodic.task.class` | string | "org.zstack.core.host.HeartbeatChecker" |
| `periodic.interval` | long | 30 |
| `periodic.time_unit` | string | "SECONDS" |

#### 2.2.10 CancelablePeriodicTask (INTERNAL span)

**When**: Executing cancelable periodic task

**Span Name**: `CancelablePeriodicTask: {TASK_NAME}`

**Required Attributes**:
| Attribute | Type | Example |
|-----------|------|---------|
| `periodic.task.name` | string | "PollingTask" |
| `periodic.task.class` | string | "org.zstack.core.polling.PollingTask" |
| `periodic.interval` | long | 5 |
| `periodic.time_unit` | string | "SECONDS" |
| `periodic.cancelable` | boolean | true |
| `periodic.cancelled` | boolean | false (set to true if cancelled) |

---

## 3. Agent Implementation (Python/Go)

### 3.1 HTTP Server (Agent Receiving Requests)

When agent receives HTTP request from management node:

1. **Extract Context**: Parse `traceparent`/`tracestate` from HTTP headers
2. **Create SERVER Span**: With extracted context as parent
3. **Execute Handler**: Within span scope
4. **Set Status**: OK or ERROR based on result
5. **End Span**

```python
# Python Example
from opentelemetry import trace
from opentelemetry.trace import SpanKind, StatusCode
from opentelemetry.propagate import extract

def handle_request(request):
    # Extract context from headers
    context = extract(request.headers)
    
    tracer = trace.get_tracer("zstack-agent")
    
    with tracer.start_as_current_span(
        f"Agent {request.method} {request.path}",
        context=context,
        kind=SpanKind.SERVER,
        attributes={
            "http.method": request.method,
            "http.path": request.path,
            "http.url": request.url,
        }
    ) as span:
        try:
            result = process_command(request)
            span.set_status(StatusCode.OK)
            return result
        except Exception as e:
            span.record_exception(e)
            span.set_status(StatusCode.ERROR, str(e))
            raise
```

### 3.2 Span Naming Convention for Agents

| Operation | Span Name Format | Example |
|-----------|------------------|---------|
| Receive HTTP | `Agent {METHOD} {PATH}` | `Agent POST /vm/start` |
| Shell command | `Shell: {COMMAND}` | `Shell: virsh start` |
| File operation | `File: {OPERATION}` | `File: write /etc/config` |
| API call | `API: {TARGET}` | `API: libvirt.createVM` |

### 3.3 Required Attributes for Agent Spans

#### Agent HTTP Handler
| Attribute | Required | Description |
|-----------|----------|-------------|
| `http.method` | YES | HTTP method |
| `http.path` | YES | Request path |
| `http.status_code` | YES | Response status |
| `agent.type` | YES | Agent type (e.g., "kvm", "ceph") |
| `host.uuid` | NO | Host UUID if available |

#### Shell Command Execution
| Attribute | Required | Description |
|-----------|----------|-------------|
| `shell.command` | YES | Command executed (sanitized) |
| `shell.exit_code` | YES | Exit code |
| `shell.timeout` | NO | Timeout in seconds |

---

## 4. Sampling Strategy

### 4.1 Environment-Based Sampling

| Environment | Sampling Rate | Description |
|-------------|---------------|-------------|
| `DEV` | 100% | Full sampling for development |
| `TEST` | 100% | Full sampling for testing |
| `STAGING` | 10% minimum | At least 10%, configurable higher |
| `PROD` | Configurable (default 1%) | Low overhead for production |

### 4.2 Error Retention (Tail-Based Sampling)

When `alwaysSampleErrors=true`:
- Initial decision uses rate-based sampling
- If span ends with ERROR status, export regardless of initial decision
- Ensures all errors are captured even with low sampling rate

### 4.3 Configuration

```properties
# Management Node (Java)
Telemetry.enabled=true
Telemetry.environment=PROD
Telemetry.samplingRate=0.01
Telemetry.alwaysSampleErrors=true
```

```yaml
# Agent (Python/Go)
telemetry:
  enabled: true
  environment: PROD
  sampling_rate: 0.01
  always_sample_errors: true
```

---

## 5. Metrics

### 5.1 Overview

ZStack exposes Prometheus-compatible metrics for monitoring thread pool health and task queue status. These metrics enable real-time dashboards and alerting for high-concurrency scenarios (e.g., 1000+ concurrent host operations).

### 5.2 Configuration

```properties
# Management Node (zstack.properties)
Telemetry.enabled=true
Telemetry.metricsEnabled=true
Telemetry.prometheusPort=9464
Telemetry.metricsCollectionIntervalSeconds=15
Telemetry.maxTrackedSignatures=500
```

### 5.3 Prometheus Endpoint

When enabled, metrics are exposed at:
```
http://{management-node}:9464/metrics
```

Prometheus scrape configuration:
```yaml
scrape_configs:
  - job_name: 'zstack'
    static_configs:
      - targets: ['management-node:9464']
    scrape_interval: 15s
```

### 5.4 Available Metrics

#### Thread Pool Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `zstack_threadpool_active` | Gauge | `pool` | Active thread count |
| `zstack_threadpool_size` | Gauge | `pool` | Current pool size |
| `zstack_threadpool_max_size` | Gauge | `pool` | Maximum pool size |
| `zstack_threadpool_queue_size` | Gauge | `pool` | Tasks waiting in queue |
| `zstack_threadpool_completed_total` | Gauge | `pool` | Total completed tasks |

Pool names: `main`, `sync`, or custom registered pool names.

#### ChainTask Queue Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `zstack_chaintask_pending` | Gauge | `signature` | Pending chain tasks per queue |
| `zstack_chaintask_running` | Gauge | `signature` | Running chain tasks per queue |

#### SyncTask Queue Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `zstack_synctask_pending` | Gauge | `signature` | Pending sync tasks per queue |
| `zstack_synctask_running` | Gauge | `signature` | Running sync tasks per queue |

#### Task Timing Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `zstack_task_wait_time_ms` | Histogram | `task_type`, `signature` | Time tasks spend waiting in queue |
| `zstack_task_execution_time_ms` | Histogram | `task_type`, `signature` | Task execution duration |
| `zstack_task_submitted_total` | Counter | `task_type`, `signature` | Total submitted tasks |
| `zstack_task_completed_total` | Counter | `task_type`, `signature`, `status` | Total completed tasks (status: success/error) |

### 5.5 Grafana Dashboard Example

```
┌──────────────────────────────────────────────────────────────┐
│  ZStack Thread Pool Status (1000 hosts concurrent)          │
├──────────────────────────────────────────────────────────────┤
│  Active Threads: ████████████████░░░░ 800/1000 (80%)        │
│  Queue Depth:    ████████████████████ 2500 pending          │
│                                                              │
│  Top 5 Busy Queues:                                          │
│  1. host-connect-queue:     ████████ 450 pending            │
│  2. vm-start-queue:         ██████   320 pending            │
│  3. storage-allocate-queue: █████    280 pending            │
│                                                              │
│  Task Wait Time (p99): 12.5s                                 │
│  Task Exec Time (p99): 2.3s                                  │
└──────────────────────────────────────────────────────────────┘
```

### 5.6 PromQL Query Examples

```promql
# Thread pool utilization
zstack_threadpool_active{pool="main"} / zstack_threadpool_max_size{pool="main"}

# Top 10 busiest chain task queues
topk(10, zstack_chaintask_pending)

# Task queue growth rate (tasks/sec)
rate(zstack_task_submitted_total[5m])

# Error rate by queue
rate(zstack_task_completed_total{status="error"}[5m]) 
  / rate(zstack_task_completed_total[5m])

# P99 wait time for chain tasks
histogram_quantile(0.99, rate(zstack_task_wait_time_ms_bucket{task_type="chain"}[5m]))
```

### 5.7 Alerting Rules Example

```yaml
groups:
  - name: zstack-threadpool
    rules:
      - alert: ThreadPoolExhausted
        expr: zstack_threadpool_active / zstack_threadpool_max_size > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Thread pool {{ $labels.pool }} is > 90% utilized"
          
      - alert: LargeTaskQueueBacklog
        expr: zstack_chaintask_pending > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Chain task queue {{ $labels.signature }} has > 1000 pending tasks"
          
      - alert: HighTaskWaitTime
        expr: histogram_quantile(0.99, rate(zstack_task_wait_time_ms_bucket[5m])) > 30000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Task wait time P99 > 30 seconds"
```

### 5.8 Cardinality Control

To prevent metric explosion from high-cardinality labels:

1. **Max Tracked Signatures**: `Telemetry.maxTrackedSignatures=500` limits unique queue signatures tracked. Signatures beyond this limit are aggregated under label value `"other"`.

2. **Signature Truncation**: Signatures longer than 100 characters are truncated.

3. **Periodic Cleanup**: Metrics for inactive queues (queue size = 0) are naturally cleaned up when the queue wrapper is removed.

### 5.9 Task Timing Implementation

Task timing metrics are recorded inline during task execution in `DispatchQueueImpl`:

| Task Type | Wait Time Recorded | Execution Time Recorded |
|-----------|-------------------|------------------------|
| `ChainTask` | When task moves from pending to running queue | In callback when task completes |
| `SyncTask` | At start of `SyncTaskFuture.run()` | In finally block of `SyncTaskFuture.run()` |
| `SingleFlightTask` | When `runSingleFlight()` starts execution | In `executeSingleRunTasks()` callback |

Wait time is calculated as: `startExecutionTime - startPendingTime`
Execution time is calculated as: `currentTime - startExecutionTime`

The `incrementTaskCompleted()` counter is also recorded with success/error status based on whether an exception occurred during execution.

---

## 6. Exporters

### 6.1 OTLP (Recommended)

Primary exporter for all implementations.

**Java Configuration**:
```properties
Telemetry.exporters=otlp
Telemetry.otlpEndpoint=http://jaeger:4317
```

**Python Configuration**:
```python
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

exporter = OTLPSpanExporter(endpoint="http://jaeger:4317")
```

### 6.2 Sentry (Optional)

Secondary exporter for error tracking integration.

**Java**: Uses reflection, optional dependency
**Python**: Use `sentry-sdk[opentelemetry]`

**Sentry 展示对齐**：CloudBus 使用可搜索的 span 名（`CloudBus Send: ...`、`CloudBus Handle: ...`），便于在 Sentry Performance 中按「CloudBus Handle」等关键词搜索；同时设置 OTel/Sentry 语义属性 `messaging.destination.name`、`messaging.message.id` 及 `messaging.system`、`messaging.message_class`，便于筛选与分组。

---

## 7. Semantic Conventions

Follow [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/).

### 7.1 Service Identification

| Attribute | Value | Description |
|-----------|-------|-------------|
| `service.name` | `zstack-management-node` | Management node |
| `service.name` | `zstack-kvm-agent` | KVM agent |
| `service.name` | `zstack-ceph-agent` | Ceph agent |
| `service.version` | `5.5.0` | Version string |
| `deployment.environment` | `DEV/TEST/STAGING/PROD` | Environment |

### 7.2 ZStack-Specific Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `zstack.api.name` | string | API message class name |
| `zstack.api.id` | string | API request UUID |
| `zstack.resource.uuid` | string | Resource UUID being operated |
| `zstack.resource.type` | string | Resource type (VmInstance, Volume, etc.) |
| `zstack.chain.signature` | string | ChainTask queue signature |
| `zstack.host.uuid` | string | Host UUID |

---

## 8. Error Handling

### 8.1 Exception Recording

```python
# Python
span.record_exception(exception)
span.set_status(StatusCode.ERROR, str(exception))
```

```java
// Java
span.recordException(throwable);
span.setStatus(StatusCode.ERROR, throwable.getMessage());
```

### 8.2 Error Span Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `exception.type` | string | Exception class name |
| `exception.message` | string | Exception message |
| `exception.stacktrace` | string | Full stack trace |
| `error.type` | string | Error category |

---

## 9. Implementation Checklist

### 9.1 Management Node (Java) ✓

- [x] TelemetryFacade interface and implementation
- [x] W3C context propagation (MessageTracingHelper)
- [x] ZStackSampler with environment-aware sampling
- [x] ErrorKeepingSpanProcessor for error retention
- [x] Pluggable exporters (OTLP, Sentry)
- [x] RestServer API entry spans
- [x] RESTFacadeImpl HTTP client spans
- [x] DispatchQueueImpl ChainTask spans (with success/error status)
- [x] DispatchQueueImpl SyncTask spans
- [x] @Traced annotation and TracingAspect
- [x] SimpleFlowChain FlowChain/Flow/Rollback spans
- [x] ThreadFacadeImpl Task spans
- [x] ThreadFacadeImpl PeriodicTask spans
- [x] ThreadFacadeImpl CancelablePeriodicTask spans
- [x] TelemetryMetricsFacade for Prometheus metrics
- [x] Thread pool metrics (active, size, max, queue, completed)
- [x] ChainTask queue metrics (pending, running)
- [x] SyncTask queue metrics (pending, running)
- [x] Task timing histograms (wait time, execution time)
- [x] Periodic metrics collection in ThreadFacadeImpl
- [x] CloudBus message spans (PRODUCER/CONSUMER)

### 9.2 Agent (Python) TODO

- [ ] OpenTelemetry SDK setup
- [ ] Context extraction from HTTP headers
- [ ] SERVER spans for incoming requests
- [ ] INTERNAL spans for command execution
- [ ] Error recording and status setting
- [ ] OTLP exporter configuration
- [ ] Environment-aware sampling
- [ ] Configuration file support

---

## 10. Trace Flow Example

### 10.1 Basic Trace Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Complete Trace Flow                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Client                  Management Node                      KVM Agent     │
│    │                           │                                  │         │
│    │ POST /v1/vm-instances     │                                  │         │
│    │ ─────────────────────────>│                                  │         │
│    │                           │                                  │         │
│    │                    ┌──────┴──────┐                           │         │
│    │                    │ SERVER Span │ API POST /v1/vm-instances │         │
│    │                    │  (root)     │                           │         │
│    │                    └──────┬──────┘                           │         │
│    │                           │                                  │         │
│    │                    ┌──────┴──────┐                           │         │
│    │                    │ INTERNAL    │ ChainTask: StartVmInstance│         │
│    │                    │ Span        │                           │         │
│    │                    └──────┬──────┘                           │         │
│    │                           │                                  │         │
│    │                           │ POST /vm/start                   │         │
│    │                    ┌──────┴──────┐ traceparent: 00-abc-123-01│         │
│    │                    │ CLIENT Span │─────────────────────────> │         │
│    │                    │ HTTP POST   │                    ┌──────┴──────┐  │
│    │                    └──────┬──────┘                    │ SERVER Span │  │
│    │                           │                           │ Agent POST  │  │
│    │                           │                           └──────┬──────┘  │
│    │                           │                           ┌──────┴──────┐  │
│    │                           │                           │ INTERNAL    │  │
│    │                           │                           │ Shell: virsh│  │
│    │                           │                           └──────┬──────┘  │
│    │                           │                                  │         │
│    │                           │<─────────────────────────────────│         │
│    │                           │                                  │         │
│    │<──────────────────────────│                                  │         │
│    │      200 OK               │                                  │         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.2 Complete End-to-End Trace Example: CreateVmInstance

This example shows the full trace hierarchy from API request to agent response to API completion.

```
Trace ID: 0af7651916cd43dd8448eb211c80319c
Total Duration: 3500ms

[SERVER] API POST /v1/vm-instances ──────────────────────────────────────────── 3500ms
│   http.method=POST
│   http.path=/v1/vm-instances  
│   http.status_code=200
│   http.client_ip=192.168.1.100
│
├──[INTERNAL] ChainTask: api.create.vm ─────────────────────────────────────── 3400ms
│   │   chain.signature=api.create.vm
│   │   chain.task.name=CreateVmInstanceMsg
│   │   chain.task.class=org.zstack.compute.vm.VmCreator
│   │
│   └──[INTERNAL] FlowChain: CreateVmInstance ──────────────────────────────── 3300ms
│       │   flowchain.id=FCID_a1b2c3d4
│       │   flowchain.name=CreateVmInstance
│       │   flowchain.flow_count=8
│       │
│       ├──[INTERNAL] Flow: AllocatePrimaryStorageFlow ─────────────────────── 150ms
│       │       flow.name=AllocatePrimaryStorageFlow
│       │       flow.class=org.zstack.storage.primary.AllocatePrimaryStorageFlow
│       │       flow.index=1
│       │
│       ├──[INTERNAL] Flow: AllocateHostFlow ───────────────────────────────── 200ms
│       │       flow.name=AllocateHostFlow
│       │       flow.class=org.zstack.compute.allocator.AllocateHostFlow
│       │       flow.index=2
│       │
│       ├──[INTERNAL] Flow: CreateVolumeFlow ───────────────────────────────── 800ms
│       │   │   flow.name=CreateVolumeFlow
│       │   │   flow.class=org.zstack.storage.volume.CreateVolumeFlow
│       │   │   flow.index=3
│       │   │
│       │   └──[CLIENT] HTTP POST /primarystorage/createvolume ─────────────── 750ms
│       │       │   http.method=POST
│       │       │   http.url=http://10.0.0.5:7070/primarystorage/createvolume
│       │       │   http.status_code=200
│       │       │   net.peer.name=10.0.0.5
│       │       │   net.peer.port=7070
│       │       │
│       │       └──[SERVER] Agent POST /primarystorage/createvolume ────────── 700ms (Agent)
│       │           │   http.method=POST
│       │           │   http.path=/primarystorage/createvolume
│       │           │   agent.type=ceph
│       │           │
│       │           └──[INTERNAL] Shell: rbd create ────────────────────────── 650ms (Agent)
│       │                   shell.command=rbd create pool/volume-xxx --size 100G
│       │                   shell.exit_code=0
│       │
│       ├──[INTERNAL] Flow: AllocateNicFlow ────────────────────────────────── 100ms
│       │       flow.name=AllocateNicFlow
│       │       flow.class=org.zstack.network.l3.AllocateNicFlow
│       │       flow.index=4
│       │
│       ├──[INTERNAL] Flow: CreateVmOnKvmFlow ──────────────────────────────── 1800ms
│       │   │   flow.name=CreateVmOnKvmFlow  
│       │   │   flow.class=org.zstack.kvm.CreateVmOnKvmFlow
│       │   │   flow.index=5
│       │   │
│       │   └──[CLIENT] HTTP POST /vm/create ───────────────────────────────── 1750ms
│       │       │   http.method=POST
│       │       │   http.url=http://10.0.0.10:7070/vm/create
│       │       │   http.status_code=200
│       │       │   net.peer.name=10.0.0.10
│       │       │   net.peer.port=7070
│       │       │
│       │       └──[SERVER] Agent POST /vm/create ──────────────────────────── 1700ms (Agent)
│       │           │   http.method=POST
│       │           │   http.path=/vm/create
│       │           │   agent.type=kvm
│       │           │   host.uuid=host-uuid-xxx
│       │           │
│       │           ├──[INTERNAL] Shell: qemu-img convert ──────────────────── 800ms (Agent)
│       │           │       shell.command=qemu-img convert -f qcow2 ...
│       │           │       shell.exit_code=0
│       │           │
│       │           └──[INTERNAL] Shell: virsh define + start ──────────────── 850ms (Agent)
│       │                   shell.command=virsh define vm.xml && virsh start vm-xxx
│       │                   shell.exit_code=0
│       │
│       ├──[INTERNAL] Flow: UpdateVmStateFlow ──────────────────────────────── 50ms
│       │       flow.name=UpdateVmStateFlow
│       │       flow.class=org.zstack.compute.vm.UpdateVmStateFlow
│       │       flow.index=6
│       │
│       └──[INTERNAL] Flow: PostCreateVmFlow ───────────────────────────────── 200ms
│               flow.name=PostCreateVmFlow
│               flow.class=org.zstack.compute.vm.PostCreateVmFlow
│               flow.index=7
│
└── Response sent to client: 200 OK
```

### 10.3 Trace Example: Failure with Rollback

When a flow fails, the trace shows both the failure and rollback spans:

```
Trace ID: 1bf8762027de54ee9559fc322d91420d
Total Duration: 2200ms
Status: ERROR

[SERVER] API POST /v1/vm-instances ──────────────────────────────────────────── 2200ms
│   http.method=POST
│   http.status_code=500
│   error.code=HOST.1001
│
├──[INTERNAL] ChainTask: api.create.vm ─────────────────────────────────────── 2100ms
│   │   status=ERROR
│   │
│   └──[INTERNAL] FlowChain: CreateVmInstance ──────────────────────────────── 2000ms
│       │   flowchain.id=FCID_x1y2z3w4
│       │   flowchain.name=CreateVmInstance
│       │   status=ERROR
│       │   error.code=HOST.1001
│       │   error.description=No available host
│       │
│       ├──[INTERNAL] Flow: AllocatePrimaryStorageFlow ─────────────────────── 150ms ✓
│       │       flow.index=1
│       │       status=OK
│       │
│       ├──[INTERNAL] Flow: AllocateHostFlow ───────────────────────────────── 200ms ✗
│       │       flow.index=2
│       │       status=ERROR
│       │       error.code=HOST.1001
│       │       error.description=No available host matching criteria
│       │
│       │   ════════════════ ROLLBACK STARTS ════════════════
│       │
│       ├──[INTERNAL] Rollback: AllocateHostFlow ───────────────────────────── 50ms
│       │       flow.name=AllocateHostFlow
│       │       flow.rollback=true
│       │       status=OK
│       │
│       └──[INTERNAL] Rollback: AllocatePrimaryStorageFlow ─────────────────── 100ms
│               flow.name=AllocatePrimaryStorageFlow
│               flow.rollback=true
│               status=OK
│
└── Response sent to client: 500 Internal Server Error
        error.code=HOST.1001
        error.details=No available host matching criteria
```

### 10.4 Span Timing Diagram

Visual representation of span timing relationships:

```
Time (ms)   0     500    1000   1500   2000   2500   3000   3500
            │      │      │      │      │      │      │      │
API SERVER  ├──────────────────────────────────────────────────┤
            │                                                  │
ChainTask   │ ├────────────────────────────────────────────┤   │
            │ │                                            │   │
FlowChain   │ │ ├──────────────────────────────────────┤   │   │
            │ │ │                                      │   │   │
Flow 1      │ │ ├──┤                                   │   │   │
            │ │    │                                   │   │   │
Flow 2      │ │    ├───┤                               │   │   │
            │ │        │                               │   │   │
Flow 3      │ │        ├────────────┤                  │   │   │
            │ │        │            │                  │   │   │
  HTTP      │ │        │ ├────────┤ │                  │   │   │
            │ │        │ │ Agent  │ │                  │   │   │
            │ │        │ └────────┘ │                  │   │   │
            │ │        └────────────┘                  │   │   │
Flow 4      │ │                     ├─┤                │   │   │
            │ │                       │                │   │   │
Flow 5      │ │                       ├─────────────────────┤  │
            │ │                       │                │   │   │
  HTTP      │ │                       │ ├───────────────────┤  │
            │ │                       │ │    Agent     │   │   │
            │ │                       │ └───────────────────┘  │
            │ │                       └─────────────────────┘  │
Flow 6      │ │                                          ├─┤   │
Flow 7      │ │                                            ├──┤│
            │ └────────────────────────────────────────────────┘│
            └──────────────────────────────────────────────────┘
            
Legend: 
├──┤ = Span duration
│  │ = Nested child span
```

### 10.5 JSON Trace Export Example

Example of exported trace in OTLP JSON format:

```json
{
  "resourceSpans": [{
    "resource": {
      "attributes": [
        {"key": "service.name", "value": {"stringValue": "zstack-management-node"}},
        {"key": "deployment.environment", "value": {"stringValue": "PROD"}}
      ]
    },
    "scopeSpans": [{
      "scope": {"name": "org.zstack"},
      "spans": [
        {
          "traceId": "0af7651916cd43dd8448eb211c80319c",
          "spanId": "b7ad6b7169203331",
          "parentSpanId": "",
          "name": "API POST /v1/vm-instances",
          "kind": 2,
          "startTimeUnixNano": "1706140800000000000",
          "endTimeUnixNano": "1706140803500000000",
          "attributes": [
            {"key": "http.method", "value": {"stringValue": "POST"}},
            {"key": "http.path", "value": {"stringValue": "/v1/vm-instances"}},
            {"key": "http.status_code", "value": {"intValue": "200"}}
          ],
          "status": {"code": 1}
        },
        {
          "traceId": "0af7651916cd43dd8448eb211c80319c",
          "spanId": "c8be7c8270314442",
          "parentSpanId": "b7ad6b7169203331",
          "name": "FlowChain: CreateVmInstance",
          "kind": 3,
          "startTimeUnixNano": "1706140800100000000",
          "endTimeUnixNano": "1706140803400000000",
          "attributes": [
            {"key": "flowchain.id", "value": {"stringValue": "FCID_a1b2c3d4"}},
            {"key": "flowchain.name", "value": {"stringValue": "CreateVmInstance"}},
            {"key": "flowchain.flow_count", "value": {"intValue": "8"}}
          ],
          "status": {"code": 1}
        },
        {
          "traceId": "0af7651916cd43dd8448eb211c80319c",
          "spanId": "d9cf8d9381425553",
          "parentSpanId": "c8be7c8270314442",
          "name": "Flow: CreateVmOnKvmFlow",
          "kind": 3,
          "startTimeUnixNano": "1706140801500000000",
          "endTimeUnixNano": "1706140803300000000",
          "attributes": [
            {"key": "flow.name", "value": {"stringValue": "CreateVmOnKvmFlow"}},
            {"key": "flow.index", "value": {"intValue": "5"}}
          ],
          "status": {"code": 1}
        },
        {
          "traceId": "0af7651916cd43dd8448eb211c80319c",
          "spanId": "e0dg9e0492536664",
          "parentSpanId": "d9cf8d9381425553",
          "name": "HTTP POST",
          "kind": 4,
          "startTimeUnixNano": "1706140801550000000",
          "endTimeUnixNano": "1706140803250000000",
          "attributes": [
            {"key": "http.method", "value": {"stringValue": "POST"}},
            {"key": "http.url", "value": {"stringValue": "http://10.0.0.10:7070/vm/create"}},
            {"key": "http.status_code", "value": {"intValue": "200"}}
          ],
          "status": {"code": 1}
        }
      ]
    }]
  }]
}
```

---

## 11. Python Agent Reference Implementation

```python
"""
ZStack Agent Telemetry Module
Implements telemetry specification for Python agents
"""

import os
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.propagate import extract, set_global_textmap
from opentelemetry.propagators.composite import CompositeHTTPPropagator
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from opentelemetry.trace import SpanKind, StatusCode


class ZStackAgentTelemetry:
    """Telemetry facade for ZStack agents"""
    
    def __init__(self, config: dict):
        self.enabled = config.get('enabled', False)
        self.environment = config.get('environment', 'DEV')
        self.sampling_rate = config.get('sampling_rate', 1.0)
        self.service_name = config.get('service_name', 'zstack-agent')
        self.otlp_endpoint = config.get('otlp_endpoint', '')
        
        self._tracer = None
        
        if self.enabled:
            self._initialize()
    
    def _initialize(self):
        # Set up W3C Trace Context propagator
        set_global_textmap(CompositeHTTPPropagator([
            TraceContextTextMapPropagator()
        ]))
        
        # Create resource
        resource = Resource.create({
            "service.name": self.service_name,
            "deployment.environment": self.environment,
        })
        
        # Create tracer provider with sampler
        provider = TracerProvider(
            resource=resource,
            sampler=self._create_sampler()
        )
        
        # Add OTLP exporter if configured
        if self.otlp_endpoint:
            exporter = OTLPSpanExporter(endpoint=self.otlp_endpoint)
            provider.add_span_processor(BatchSpanProcessor(exporter))
        
        trace.set_tracer_provider(provider)
        self._tracer = trace.get_tracer("org.zstack.agent")
    
    def _create_sampler(self):
        from opentelemetry.sdk.trace.sampling import (
            TraceIdRatioBased, 
            ParentBasedTraceIdRatio
        )
        
        if self.environment in ('DEV', 'TEST'):
            rate = 1.0
        elif self.environment == 'STAGING':
            rate = max(self.sampling_rate, 0.1)
        else:
            rate = self.sampling_rate
        
        return ParentBasedTraceIdRatio(rate)
    
    @property
    def tracer(self):
        return self._tracer
    
    def extract_context(self, headers: dict):
        """Extract trace context from HTTP headers"""
        return extract(headers)
    
    def start_server_span(self, name: str, headers: dict, attributes: dict = None):
        """Start a SERVER span for incoming HTTP request"""
        context = self.extract_context(headers)
        return self._tracer.start_span(
            name,
            context=context,
            kind=SpanKind.SERVER,
            attributes=attributes or {}
        )
    
    def start_internal_span(self, name: str, attributes: dict = None):
        """Start an INTERNAL span for internal operations"""
        return self._tracer.start_span(
            name,
            kind=SpanKind.INTERNAL,
            attributes=attributes or {}
        )


# Flask integration example
def create_tracing_middleware(telemetry: ZStackAgentTelemetry):
    """Create Flask middleware for automatic tracing"""
    
    def middleware(app):
        @app.before_request
        def before_request():
            from flask import request, g
            
            if not telemetry.enabled:
                return
            
            span = telemetry.start_server_span(
                f"Agent {request.method} {request.path}",
                dict(request.headers),
                {
                    "http.method": request.method,
                    "http.path": request.path,
                    "http.url": request.url,
                    "agent.type": telemetry.service_name,
                }
            )
            g.trace_span = span
            g.trace_token = trace.use_span(span, end_on_exit=False)
            g.trace_token.__enter__()
        
        @app.after_request
        def after_request(response):
            from flask import g
            
            if hasattr(g, 'trace_span') and g.trace_span:
                g.trace_span.set_attribute("http.status_code", response.status_code)
                if response.status_code >= 400:
                    g.trace_span.set_status(StatusCode.ERROR)
                else:
                    g.trace_span.set_status(StatusCode.OK)
            
            return response
        
        @app.teardown_request
        def teardown_request(exception):
            from flask import g
            
            if hasattr(g, 'trace_span') and g.trace_span:
                if exception:
                    g.trace_span.record_exception(exception)
                    g.trace_span.set_status(StatusCode.ERROR, str(exception))
                
                g.trace_token.__exit__(None, None, None)
                g.trace_span.end()
        
        return app
    
    return middleware
```

---

## Appendix A: Configuration Reference

### Management Node (zstack.properties)

**注意**：等号两边不要加空格（`key=value`），否则 key 会变成带空格的，读不到，会用默认值（如 `Telemetry.enabled` 默认 false）。
管理节点上遥测**不会**监听 OTLP/Sentry 端口，只会向外发送；本地会监听的只有 **9464**（Prometheus /metrics）。

```properties
# Enable/disable telemetry
Telemetry.enabled=true

# Environment: DEV, TEST, STAGING, PROD
Telemetry.environment=PROD

# Sampling rate (0.0 to 1.0)
Telemetry.samplingRate=0.01

# Always export error spans
Telemetry.alwaysSampleErrors=true

# Exporters (comma-separated): otlp, sentry
Telemetry.exporters=otlp

# Sentry DSN (when using sentry exporter). Also used by CloudBus error reporting.
# Can be set here or via -Dsentry.dsn=... or env SENTRY_DSN (no spaces around =)
# Telemetry.sentryDsn=https://your-key@your-org.ingest.sentry.io/project-id

# Sentry traces sample rate (0.0 to 1.0). Must be set in Sentry.init() or Performance/Traces may be empty.
Telemetry.sentryTracesSampleRate=1.0

# OTLP endpoint
Telemetry.otlpEndpoint=http://jaeger:4317

# Service identification
Telemetry.serviceName=zstack-management-node
Telemetry.serviceVersion=5.5.0

# Batching configuration
Telemetry.maxExportBatchSize=512
Telemetry.exportDelayMs=5000
Telemetry.maxQueueSize=2048

# Metrics configuration
Telemetry.metricsEnabled=true
Telemetry.prometheusPort=9464
Telemetry.metricsCollectionIntervalSeconds=15
Telemetry.maxTrackedSignatures=500
```

### Agent (agent.yaml)

```yaml
telemetry:
  enabled: true
  environment: PROD
  sampling_rate: 0.01
  always_sample_errors: true
  
  otlp:
    endpoint: "http://jaeger:4317"
    insecure: true
  
  service:
    name: zstack-kvm-agent
    version: "5.5.0"
  
  batch:
    max_queue_size: 2048
    max_export_batch_size: 512
    export_timeout_ms: 30000
```

---

## Appendix B: Sentry 对接与测试

对接公司 Sentry 时按以下步骤配置和验证。

**配置注意**：`zstack.properties` 中等号两边**不要加空格**（`key=value`），否则 key 带空格读不到；Sentry 测试时建议 `Telemetry.exporters=otlp,sentry`、`Telemetry.sentryTracesSampleRate=1.0`（或通过该配置项设置）。使用 Sentry 时需在 **Sentry.init()** 中设置 **tracesSampleRate**，否则 Sentry 可能不接收 OTel 发去的 transaction，Performance/Traces 可能看不到数据；ZStack 已通过 `Telemetry.sentryTracesSampleRate` 传入，默认 1.0。

### B.1 前置条件

- 已拿到 Sentry 项目的 **DSN**（形如 `https://xxx@xxx.ingest.sentry.io/xxx`）。
- 管理节点 classpath 中包含 `sentry`、`sentry-opentelemetry-core`（根 `pom.xml` 的 dependencyManagement 中已声明，build 打包时会带入 WEB-INF/lib）。**注意**：`sentry-opentelemetry-bootstrap` 已被显式排除，以避免其抢先注册 GlobalOpenTelemetry，导致与 ZStack 自建的 TracerProvider（含 Sentry exporter）冲突。

### B.2 配置步骤

**1. 配置 DSN**

Sentry 由 `SentryInitHelper.initIfNeeded()` 在 TelemetryFacade 构建 OpenTelemetry SDK 之后调用 `Sentry.init()` 进行初始化。DSN 来源（任选其一，优先级：Telemetry.sentryDsn > sentry.dsn 系统属性 > SENTRY_DSN 环境变量）：

- **推荐** 在 `zstack.properties` 中增加（会通过 `System.getProperties().load()` 注入为系统属性）：
  ```properties
  sentry.dsn=https://你的key@你的org.ingest.sentry.io/项目id
  ```
- 或启动 JVM 时加参数：`-Dsentry.dsn=https://...`
- 或设置环境变量：`SENTRY_DSN=https://...`

**2. 开启 Sentry 与 Telemetry Sentry 导出**

在 `zstack.properties` 中增加或修改：

```properties
# 启用 Sentry（TelemetryFacade 启动时在构建 OTel SDK 后通过 SentryInitHelper.initIfNeeded() 调用 Sentry.init()；必须为 true 才能把 trace 发到 Sentry）
CloudBus.sentryOn=true

# 启用遥测并启用 sentry exporter
Telemetry.enabled=true
Telemetry.exporters=sentry

# 可选：仅用 Sentry 时可不必配 OTLP；若同时用 OTLP 与 Sentry，可写：
# Telemetry.exporters=otlp,sentry
```

**3. 测试环境建议**

本地/测试环境可提高采样率以便快速看到 trace；Sentry 端需设置 tracesSampleRate（ZStack 通过 `Telemetry.sentryTracesSampleRate` 传入，默认 1.0）：

```properties
Telemetry.environment=DEV
Telemetry.samplingRate=1.0
Telemetry.sentryTracesSampleRate=1.0
Telemetry.alwaysSampleErrors=true
```

### B.3 验证方式

**方式一：看启动日志**

启动管理节点后查看日志，应出现类似：

- `Sentry initialized (tracesSampleRate=...)`（由 SentryInitHelper 在 TelemetryFacade 构建 OTel SDK 后输出，表示 Sentry.init() 成功且已设置 tracesSampleRate）。
- `Telemetry initialized successfully: environment=..., exporters=sentry` 或 `exporters=otlp,sentry`。
- `Enabled exporter: sentry`（表示已启用 sentry exporter）；首次有 span 导出时会出现 `LazySentryExporter: created Sentry span exporter (first use)`。

若出现 `Sentry exporter requested but sentry-opentelemetry-core is not on classpath` 或 exporter 不可用相关告警，说明依赖或配置有误，需检查打包/classpath 及 `CloudBus.sentryOn`、`Telemetry.sentryDsn`（或 sentry.dsn/SENTRY_DSN）。

**方式二：触发带错误的请求**

- 调用一个会返回错误或抛异常的 API（例如错误参数、不存在的资源 UUID）。
- 由于配置了 `Telemetry.alwaysSampleErrors=true`，带错误的 span 会被强制采样并导出。
- 在 Sentry 控制台 **Issues** 或 **Performance/Traces** 中查看是否出现对应错误或 trace。

**方式三：DEV 环境全量采样**

- 将 `Telemetry.environment=DEV`、`Telemetry.samplingRate=1.0` 时，所有请求都会被采样。
- 正常调用若干 API（如列出云主机、创建/删除测试资源），然后在 Sentry 的 **Performance** / **Traces** 中查看是否有对应 trace。

### B.4 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 控制台没有 trace/error | DSN 未生效 | 确认 `sentry.dsn` 已在 zstack.properties 或 -D/环境变量中设置，且 `CloudBus.sentryOn=true` |
| 没有 Sentry exporter | 依赖未带入 | 确认运行时 classpath 包含 `sentry-opentelemetry-core`，必要时去掉 optional 或调整打包 |
| 只有 error 没有 trace | 采样率低且请求未报错；或 Sentry 未设置 tracesSampleRate | 在测试环境用 `Telemetry.environment=DEV`、`Telemetry.samplingRate=1.0`、`Telemetry.sentryTracesSampleRate=1.0`，或故意触发错误并用 `alwaysSampleErrors=true` 验证；确认 Sentry.init() 已设置 tracesSampleRate（ZStack 通过 Telemetry.sentryTracesSampleRate 传入） |
