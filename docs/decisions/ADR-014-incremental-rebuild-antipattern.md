# ADR-014 — Incremental rebuild antipattern → 铁律 12 + harness 守门

**Status**: Accepted — 2026-04-27
**Supersedes**: none
**Superseded by**: none

## Context

Phase 2D 期间反复（5+ 次）撞 `java.lang.VerifyError: Bad type on operand stack` → "The forked VM terminated without properly saying goodbye" 启动失败。

根因链:

1. 改 `header/` 或共享 entity（VO/AO）source
2. 跑 `mvn install -pl <my-module>,compute,plugin/physicalServer,... -am`（**无 `clean`**）
3. Maven mtime check：`compute` 等下游 module source 没变 → 标 up-to-date → **不重新编译**
4. 但 `compute` 的 AspectJ-woven `lambda$1` 引用了 `header` 的旧 bytecode signature
5. 启动时 JVM verifier 校验 method type signature → unmatched → `VerifyError`

裸 `mvn install -pl X -am` 在跨模块 entity 改动场景是反模式。`mvn -am` 只重建 X 的**直接**上游依赖，下游 woven module 不被认为需要重建（mtime 没变）。但 AspectJ post-compile weaving 在 X 改 entity 时使下游 woven bytecode 需要重新生成。

## Decision

**铁律 12 (CLAUDE.md)**：改 `header/` 或任何共享 VO/AO 后**必须**:

```bash
mvn clean install \
  -pl <my-module>,compute,plugin/physicalServer,plugin/kvm,premium/baremetal2 \
  -am -P premium
```

`clean` 强制下游 woven module 重建，绕过 mtime 假阴。

**Harness 守门 (per-dev opt-in，不 commit 到仓)**:

1. **`./scripts/mvn-safe-install.sh -pl X,Y -am`** — 包装脚本：检测 `header/src/main/java/**` + `abstraction/src/main/java/**` + `**/*VO.java` + `**/*AO.java` 修改时间是否新于 `compute` jar 的 mtime。是 → 强制 `clean install`；否 → 透传给原 `mvn install`
2. **`.claude/hooks/guard-mvn-stale.sh`** (gitignored，PreToolUse:Bash hook) — 拦截裸 `mvn install -pl X -am` 命令，检测到 stale 直接 `exit 2` 阻断。`mvn clean install` / `mvn test` / `runMavenProfile` 不受影响

**Stale-guard 范围（2026-04-27 修订）**：原版只检查 `header/` + `abstraction/`。Phase 2D 实测发现改 `premium/baremetal2/.../BareMetal2ProvisionNetworkClusterRefVO` 后 `-am` 重建拉了 zstack-iam2 / compute 等，仍裸 `mvn install`，VerifyError 重现。**guard 必须扩到 `**/*VO.java` `**/*AO.java` 跨模块**，不光 header/abstraction。

## Consequences

- **Build 慢**: `clean install` 比增量编译慢 5-10x。换：可靠不爆。Phase 2/3 节奏接受
- **Per-dev opt-in 不强制**：harness 在 `.claude/hooks/` 下 gitignored；team 成员自己决定是否启用。CLAUDE.md 铁律 12 是 minimum bar，harness 是放心兜底
- **手抖逃生**: 若 hook 误阻断（typical: `mvn install -pl X` 不 -am 的 quick rebuild），可 `OMC_SKIP_HOOKS=guard-mvn-stale ...` 单次绕过
- **替代方案**: `mvn-clean-install.sh` 别名永远 clean —— 比 stale heuristic 更傻瓜，但牺牲增量速度。Phase 3 实装阶段如果 stale-guard 还是常误判，可以切到永远 clean 的别名

## Alternatives considered

**Option B — 信任 Maven dependency tracker**：等 maven 自己探测到下游需要重建。**不可行**：mtime check 是 maven 的 contract，AspectJ post-weave 不影响 source mtime，maven 永远认为 woven bytecode 是 fresh 的。这不是 maven bug，是 AspectJ 与 maven mtime check 的语义错位。

**Option C — Symlink upstream jar 到 .m2**：手动从 build 目录 symlink 最新 jar 到本地 repo，跳过 maven install。脆弱，每次切支持一遍，Phase 2D 试过 1 次撞回 worse 错（symlink 指向 stale build target），放弃。

**Option D — 全局禁用 AspectJ weaving**：错的方向。ZStack `@DeadlockAutoRestart` / `@Transactional` 等 annotation 都靠 weaving；禁了等于禁项目核心功能。

A（铁律 + harness）选定因为：(1) 唯一无副作用的对冲方案；(2) `clean` 慢但语义清晰，开发者可预期；(3) harness 把"什么时候必须 clean"的判断从开发者脑子里搬到机器上。

## References

- 铁律 12 落点: `CLAUDE.md` "Code & API" / "Workflow" 节
- 包装脚本: `./scripts/mvn-safe-install.sh`（项目 root）
- Hook 模板: `.claude/hooks/guard-mvn-stale.sh`（gitignored，每 dev 自己启）
- 撞坑记录: `docs/brainstorms/next-session.md` §0（Phase 2D 5+ 次 VerifyError 复现）
- Phase 3 待办: 扩 stale-guard 范围到 `**/*VO.java` / `**/*AO.java` 跨模块（next-session.md §3 Blocker 5）
- 相关 ADR: 无（这是开发流程级，不影响代码层）
