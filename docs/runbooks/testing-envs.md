# Testing Environments Runbook

v5.5.18 Unified Hardware 开发涉及的测试环境、数据库、快照获取流程。

> 本文件记录**长期稳定的环境信息**，不记录某一轮 session 的测试中间态。
> 一次性测试 DB 用完即删，不进本文件。

---

## 1. 216 集成测试环境

| 项 | 值 |
|---|---|
| Host | `172.25.200.216` |
| SSH | 免密已配置（`ssh 172.25.200.216`） |
| MySQL user | `root` |
| MySQL password | `zstack.mysql.password` |
| ZStack 版本 | v4.8.36（老 Flyway，**无** 5.0.0+ migrations） |

### 数据量（基线）

| 表 | 行数 |
|---|---|
| HostCapacityVO | 10 |
| BareMetal2ProvisionNetworkVO | 1 |
| BareMetal2ProvisionNetworkClusterRefVO | 1 |
| ESXi (VcenterHostVO) | 1 |
| ClusterVO | 7 |
| ZoneVO | 3 |
| ResourceVO（总量） | ~175K |

用途：**fresh 升级 E2E** 的基线快照源。

---

## 2. 本机 MariaDB（一次性测试 DB）

| 项 | 值 |
|---|---|
| Host | `localhost` |
| User | `root` |
| Password | *(无密码)* |
| 版本 | MariaDB 10.11 |

### 约定

- **一次性 DB 命名**：`zstack_<u-unit>_<intent>_test`，例如 `zstack_u28_test`、`zstack_v5518_fresh`
- 用完即 drop，不要跨 session 保留
- **不要**把测试数据留在 `zstack`（默认 DB 名）里

### 清理命令

```bash
# 列出所有测试 DB
mysql -u root -e "SHOW DATABASES LIKE 'zstack\_%\_test';"

# 批量清理（确认过再执行）
mysql -u root -e "SHOW DATABASES LIKE 'zstack\_%'" \
  | tail -n +2 \
  | xargs -I{} mysql -u root -e "DROP DATABASE \`{}\`;"
```

---

## 3. 全量拉 216 快照（E2E 测试必备）

### ⚠️ DEFINER trap 必须预处理

mysqldump 会把 VIEW DDL 导出成 `DEFINER=<remote>@<host>`，本机 restore 触发 `ERROR 1356`
（详见 [v5518-sql-ddl-pitfalls.md pitfall #1](v5518-sql-ddl-pitfalls.md)）。

### 完整拉取脚本

```bash
# 1. 在 216 上 dump
ssh 172.25.200.216 "mysqldump -u root -pzstack.mysql.password \
    --single-transaction --skip-triggers --skip-comments --no-tablespaces \
    zstack > /tmp/zstack-216-full.sql"

# 2. 取回本地
scp 172.25.200.216:/tmp/zstack-216-full.sql /tmp/

# 3. 预处理：DEFINER → localhost，SECURITY DEFINER → INVOKER
sed 's|DEFINER=[^ ]*@[^ ]* |DEFINER=`root`@`localhost` |g;
     s|SQL SECURITY DEFINER|SQL SECURITY INVOKER|g' \
    /tmp/zstack-216-full.sql > /tmp/zstack-216-full-patched.sql

# 4. Restore 到 fresh DB
mysql -u root -e "DROP DATABASE IF EXISTS zstack_test;
                  CREATE DATABASE zstack_test CHARACTER SET utf8;"
mysql -u root zstack_test < /tmp/zstack-216-full-patched.sql

# 5. 验证
mysql -u root zstack_test -e "SELECT COUNT(*) FROM HostCapacityVO;"  # 应该 = 10
```

### 常用 subset（只拉 capacity 相关）

```bash
ssh 172.25.200.216 "mysqldump -u root -pzstack.mysql.password \
    --single-transaction --skip-triggers \
    zstack HostVO HostCapacityVO KVMHostVO BareMetal2ChassisVO \
           BareMetal2ProvisionNetworkVO VcenterHostVO ClusterVO ZoneVO \
    > /tmp/zstack-216-capacity.sql"
```

---

## 4. Flyway 升级验证的标准 5 步

在 fresh 快照上跑 `V5.5.18__schema.sql` 的验证模板：

```bash
# 1. Fresh restore（见第 3 节脚本 1-4）

# 2. 记录 pre-migration baseline
mysqldump -u root --skip-triggers --skip-comments --no-tablespaces \
  zstack_test HostCapacityVO > /tmp/hcv-pre.sql

# 3. Apply schema
mysql -u root zstack_test < /path/to/V5.5.18__schema.sql
# 期望：exit=0，< 1s（fresh 216 实测 0.32s）

# 4. 验证行数
mysql -u root zstack_test -e "
  SELECT 'PS', COUNT(*) FROM PhysicalServerVO
  UNION SELECT 'PSC', COUNT(*) FROM PhysicalServerCapacityVO
  UNION SELECT 'HCV-view', COUNT(*) FROM HostCapacityVO;
"
# 期望（216 基线）: PS=9, PSC=10 (9 KVM MD5-salted + 1 ESXi direct), HCV=10

# 5. AC-V2-MIG-04 字节级 diff（pre vs post HCV VIEW）
mysqldump -u root --skip-triggers --skip-comments --no-tablespaces \
  zstack_test HostCapacityVO > /tmp/hcv-post.sql
diff /tmp/hcv-pre.sql /tmp/hcv-post.sql
# 期望：Files are identical
```

---

## 5. 已知测试盲点

**BM2 plugin 缺失的客户**：216 有 BM2，没有 exercise "无 BM2 plugin" 的路径。
V5.5.18 Stage 3 的若干 DROP FK 对 BM2 相关表是无条件的，在无 BM2 plugin 环境会失败。
详见 [U29 rollback runbook](v5518-unified-hardware-rollback.md) 的"已知但未修"章节。
需要 `information_schema.TABLES` + prepared-statement guard 才能覆盖该分支。
