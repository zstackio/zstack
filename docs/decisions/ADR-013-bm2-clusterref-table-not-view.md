# ADR-013 — BareMetal2ProvisionNetworkClusterRefVO stays a real table for v5.5.18

**Status**: Accepted (interim) — 2026-04-27
**Supersedes**: none
**Superseded by**: later U23-U26 BM2 ProvisionNetwork pool-only rewrite

## Context

V5.5.18 STAGE 6 (`conf/db/upgrade/V5.5.18__schema.sql`, draft) converted
`BareMetal2ProvisionNetworkClusterRefVO` from a real table into a join VIEW
over `PhysicalServerProvisionNetworkPoolRefVO JOIN ClusterEO`, filtered on
`c.serverPoolUuid IS NOT NULL`. The intent was to unify BM2's per-cluster
attachment model with the open-source per-pool model in one VIEW.

`baremetal2-architect` review (2026-04-27, before any code change) found
the VIEW model fundamentally incompatible with the existing API contract:

1. **BM2 clusters are born pool-less.** `BareMetal2ClusterFactory.createCluster`
   does NOT assign a `serverPoolUuid`, and `BareMetal2ProvisionNetworkApiInterceptor`
   never enforces one at attach time. The migration's Block 0a auto-pools
   *existing* BM2 clusters at upgrade, but it is a backfill, not a runtime
   invariant. Fresh `createCluster {type:"baremetal2"}` → `attachBareMetal2ProvisionNetworkToCluster`
   produces clusters that the VIEW filter silently drops.
2. **DML on VIEW fails.** `BareMetal2ProvisionNetworkBase:413` does
   `dbf.persist(BareMetal2ProvisionNetworkClusterRefVO)` and `:615` does
   `SQL.New(...).delete()`. MySQL rejects DML on a multi-table-derived join
   VIEW (1394 / 1395).
3. **Detach semantics are undefined under VIEW.** The API is per-(network,
   cluster); the VIEW collapses identity to per-(network, pool). Detaching a
   network from one cluster cannot be expressed without affecting all
   clusters sharing the pool.
4. **16 production read sites depend on the per-cluster identity.** Read
   queries against `(networkUuid, clusterUuid)` exist in
   `BareMetal2GatewayCascadeExtension`, `BareMetal2Gateway`, `BareMetal2InstanceApiInterceptor`,
   `BareMetal2InstanceAllocateClusterFlow`, `BareMetal2ChassisApiInterceptor`,
   `BareMetal2ClusterFactory`, plus 5 in the provisionnetwork module itself.

Forcing the refactor to make the VIEW writable would require changing the
public REST API contract (require pool-first attach) and breaking those
read sites. That is U23-U26 scope, not Phase 2D.

## Decision

**Keep `BareMetal2ProvisionNetworkClusterRefVO` as a real table for v5.5.18.**
Drop STAGE 6 from the migration. Restore the entity's
`@SoftDeletionCascades` + `@ForeignKey CASCADE` annotations (reverts commit
`0c027b1204` in the premium subrepo). BM2 reads, writes, and cluster/network
cascades work exactly as in v5.5.16.

Block B1 (the PoolRef backfill from BM2 ClusterRef history) stays. It
populates the open-source `PhysicalServerProvisionNetworkPoolRefVO` so the
unified-pool path has data to read; BM2's own table remains the source of
truth for BM2 attachments.

## Consequences

- BM2 case (`Bm2RoleProviderIntegrationCase`) unblocks immediately — no
  Java production change is needed beyond restoring the cascade annotations.
- The "unified hardware pool" picture is split: open-source provision
  networks attach via PoolRef, BM2 provision networks attach via the
  per-cluster ClusterRefVO table. Two source-of-truth shapes live in
  parallel until U23-U26 lands.
- `@SoftDeletionCascades` on the BM2 ref VO restores cluster→ref and
  network→ref cleanup. The `next-session.md §3 row 4` "cleanup gap" closes
  for v5.5.18.
- The full pool-only rewrite remains the right end state. Tracking under
  Phase 2 PRD U23-U26.

## Alternatives considered

**Option B — Auto-pool in `BareMetal2ClusterFactory.createCluster`.** Mirror
the migration's Block 0a behavior at runtime so every BM2 cluster has a
1:1 pool by invariant. Medium blast radius. Still leaves detach semantics
undefined when an admin later attaches the cluster to a shared pool.
Couples header (new `ClusterCreateExtensionPoint` or similar) and BM2
cluster factory; effectively starts U23 work without finishing it.

**Option C — Full U23-U26 rewrite.** Deprecate per-cluster API, migrate
existing data, document API contract change, full QA cycle. Right
architecturally; multi-session scope, not Phase 2D.

A was chosen because the v5.5.18 release deadline owns Phase 2D. C
remains the long-term plan.

## References

- Schema: `conf/db/upgrade/V5.5.18__schema.sql` (STAGE 6 commented out, lines
  ~567-583; Block B1 unchanged at lines ~552-565)
- Java entity: `premium/baremetal2/.../BareMetal2ProvisionNetworkClusterRefVO.java`
- Reverted commit: `0c027b1204 <fix>[baremetal2]: drop join-VIEW cascade annotations`
- Production read sites: see baremetal2-architect 2026-04-27 escalation report
  in `docs/brainstorms/next-session.md` (this session's notes).
