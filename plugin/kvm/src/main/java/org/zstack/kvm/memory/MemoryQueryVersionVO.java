package org.zstack.kvm.memory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/** Monotonic query invalidation, independent of desired-policy revisions. */
@Entity @Table
public class MemoryQueryVersionVO {
    @Id @Column(length = 16) private String name;
    @Column(nullable = false) private long revision;
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public long getRevision() { return revision; }
    public void setRevision(long value) { revision = value; }
}
