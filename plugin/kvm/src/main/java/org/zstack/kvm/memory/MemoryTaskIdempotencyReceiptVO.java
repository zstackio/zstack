package org.zstack.kvm.memory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.sql.Timestamp;

/** Compact permanent tombstone for an explicitly deleted MemoryTask root. */
@Entity
@Table
public class MemoryTaskIdempotencyReceiptVO {
    @Id
    @Column(length = 32)
    private String taskUuid;
    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }

    @Column(unique = true, length = 160)
    private String requestKey;
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }

    @Column(length = 64)
    private String requestHash;
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash = value; }

    @Column(nullable = false, length = 32)
    private String originalStatus;
    public String getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(String value) { originalStatus = value; }

    @Column(nullable = false)
    private Timestamp deletedDate;
    public Timestamp getDeletedDate() { return deletedDate; }
    public void setDeletedDate(Timestamp value) { deletedDate = value; }

    @PrePersist
    private void onCreate() {
        if (deletedDate == null) { deletedDate = new Timestamp(System.currentTimeMillis()); }
    }
}
