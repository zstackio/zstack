package org.zstack.kvm.memory;

import java.util.List;

/** One DB-transactionally consistent page and its filter-set snapshot token. */
final class MemoryQueryPage<T> {
    final List<T> items;
    final long total;
    final String snapshotId;
    MemoryQueryPage(List<T> items, long total, String snapshotId) {
        this.items = items; this.total = total; this.snapshotId = snapshotId;
    }
}
