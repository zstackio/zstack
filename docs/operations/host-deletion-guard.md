# 宿主机删除保护标记的人工清理

适用分支：`4.4.52-sm-sjsguobaoce`，问题：ZSTAC-89608。

正常操作完成后，`HostDeletionGuard` 会按租约 ID 释放标记。管理节点异常退出时，标记可能留在 `zstack.JsonLabelVO` 中。宿主机删除会被阻止，错误中包含宿主机 UUID 和 `guard[id:...]`。

标记保留是为了防止旧管理节点或宿主机上的异步操作继续修改资源。管理节点记录消失、服务重启或等待超时，都不能证明旧操作已经结束。不要批量删除此类标记。

## 1. 查询指定标记

使用现场既有数据库管理方式连接 `zstack` 数据库。将以下占位符替换成错误中的值；此处只查询，不修改资源：

```sql
USE zstack;
SET @host_guard_id = <guard-id>;
SET @host_guard_host_uuid = '<host-uuid>';

SELECT id, labelKey, labelValue, resourceUuid, createDate
FROM JsonLabelVO
WHERE id = @host_guard_id
  AND resourceUuid = @host_guard_host_uuid
  AND labelKey LIKE 'hostDeletionGuard::%';

SELECT uuid, hostName, joinDate, heartBeat, state,
       CONCAT(uuid, '::', CAST(UNIX_TIMESTAMP(joinDate) * 1000 AS UNSIGNED)) AS guardOwner
FROM ManagementNodeVO;
```

`using::` 标记代表在途 VM 操作，`deleting::` 标记代表宿主机删除。`labelValue` 保存“管理节点 UUID::本次入群时间毫秒值”；同一 UUID 重启后可对应不同实例。记录第一条查询返回的完整 `labelKey` 和 `labelValue`。

若标记已不存在，直接重试产品中的正常操作，不执行后面的删除 SQL。

## 2. 确认旧操作已停止

清理前必须确认标记所属的旧管理节点实例已停止或已隔离，且其发起的挂盘、启动、迁移、恢复或宿主机删除等在途操作已经结束。还需核对相关宿主机上的实际 VM/磁盘状态与数据库绑定一致。仅查看 `ManagementNodeVO` 不足以完成这一确认。

无法确认旧进程及在途操作已经停止时，保留标记并继续排查。若所属实例仍存活且操作正在执行，应等待该操作正常释放标记。

## 3. 仅删除核实过的指定标记

完成上述确认后，在同一数据库连接中设置刚才记录的完整值。使用 ID、宿主机 UUID、标记键和值共同限定删除目标，不清理其他租约：

```sql
SET @host_guard_expected_key = '<完整 labelKey>';
SET @host_guard_expected_owner = '<完整 labelValue>';

START TRANSACTION;

SELECT id, labelKey, labelValue, resourceUuid
FROM JsonLabelVO
WHERE id = @host_guard_id
  AND resourceUuid = @host_guard_host_uuid
FOR UPDATE;

DELETE FROM JsonLabelVO
WHERE id = @host_guard_id
  AND resourceUuid = @host_guard_host_uuid
  AND labelKey = @host_guard_expected_key
  AND labelValue = @host_guard_expected_owner
  AND labelKey LIKE 'hostDeletionGuard::%';

SELECT ROW_COUNT() AS removedGuards;
```

只有查询内容与核实对象一致、且 `removedGuards = 1` 时执行：

```sql
COMMIT;
```

结果不符或影响行数不是 1 时，执行 `ROLLBACK;`，重新查询并核对对象。

## 4. 回读并重试产品操作

```sql
SELECT id, labelKey, labelValue, resourceUuid
FROM JsonLabelVO
WHERE id = @host_guard_id;
```

确认指定标记已消失后，通过产品 API/UI 重试原操作。宿主机删除仍会重新检查其他在途操作及 VM 存储；若仍有 VM 使用非本地存储，继续拒绝删除属于预期行为。若提示另一个标记，重新核实该标记，不能一次性清空所有记录。

本流程不修改 `HostDeletionGuard.release(long lease)` 的行为：该方法仍只删除传入的租约 ID。
