# ADR-001: HostCapacityUpdater.resolveServerUuidOrThrow 作为静态方法

**Status**: Accepted
**Date**: U4 实施期间
**Source**: `next-session.md` 关键决策表（U4）

## Context

U4 重写 W1-W3 capacity write path 时，需要一个位置做 `hostUuid → serverUuid` 的解析。
调用点不止 Updater 内部：API handler 前置校验、日志打点、以及跨线程的异步分支
都可能在**还没构造 Updater 实例**的情况下需要这个解析结果。

## Decision

将 `resolveServerUuidOrThrow` 实现为 `HostCapacityUpdater` 的**静态方法**，不依赖实例状态，
只通过 dbf 或传入的 `PhysicalServerCapacityVO` 完成解析。

## Consequences

- ✅ 非 Updater 路径可直接调用，不需要为了一次 uuid 解析构造整个 Updater
- ✅ API handler 的前置校验保持轻量
- ⚠️ 解析逻辑不能依赖 Updater 的 HCV cache；如果将来把 HCV cache 做成实例状态，
  这里要显式传入而不是从 this 取
- ⚠️ 找不到 server 时抛 `OperationFailureException`，调用方要有异常处理
