package org.zstack.kvm.memory;

import javax.persistence.*;

/** Durable low-frequency policy-list staging; never used by reclaim records. */
@Entity
@Table
public class MemoryTargetShardVO {
    @Id @Column(length = 32)
    private String uuid;
    @Column(nullable = false, length = 160) private String requestKey;
    @Column(nullable = false, length = 16) private String scope;
    @Column(nullable = false, length = 64) private String resourceUuid;
    @Column(nullable = false) private int shardIndex;
    @Column(nullable = false) private int shardCount;
    @Column(nullable = false) private long totalCount;
    @Column(nullable = false, length = 128) private String digest;
    @Column(nullable = false, length = 128) private String snapshotGeneration;
    @Column(nullable = false) private long expectedRevision;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String targets;
    @Column(nullable = false, length = 128) private String requestHash;
    @Column(nullable = false) private long expiresAt;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public int getShardIndex() { return shardIndex; }
    public void setShardIndex(int value) { shardIndex = value; }
    public int getShardCount() { return shardCount; }
    public void setShardCount(int value) { shardCount = value; }
    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long value) { totalCount = value; }
    public String getDigest() { return digest; }
    public void setDigest(String value) { digest = value; }
    public String getSnapshotGeneration() { return snapshotGeneration; }
    public void setSnapshotGeneration(String value) { snapshotGeneration = value; }
    public long getExpectedRevision() { return expectedRevision; }
    public void setExpectedRevision(long value) { expectedRevision = value; }
    public String getTargets() { return targets; }
    public void setTargets(String value) { targets = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { requestHash = value; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long value) { expiresAt = value; }
}
