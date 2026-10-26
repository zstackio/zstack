package org.zstack.sdk;



public class ZsDatasetTransferInventory  {

    public java.lang.String id;
    public void setId(java.lang.String id) {
        this.id = id;
    }
    public java.lang.String getId() {
        return this.id;
    }

    public java.lang.String purpose;
    public void setPurpose(java.lang.String purpose) {
        this.purpose = purpose;
    }
    public java.lang.String getPurpose() {
        return this.purpose;
    }

    public java.lang.String targetId;
    public void setTargetId(java.lang.String targetId) {
        this.targetId = targetId;
    }
    public java.lang.String getTargetId() {
        return this.targetId;
    }

    public java.lang.String filename;
    public void setFilename(java.lang.String filename) {
        this.filename = filename;
    }
    public java.lang.String getFilename() {
        return this.filename;
    }

    public long declaredSize;
    public void setDeclaredSize(long declaredSize) {
        this.declaredSize = declaredSize;
    }
    public long getDeclaredSize() {
        return this.declaredSize;
    }

    public long chunkSize;
    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }
    public long getChunkSize() {
        return this.chunkSize;
    }

    public long chunkCount;
    public void setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
    }
    public long getChunkCount() {
        return this.chunkCount;
    }

    public long receivedSize;
    public void setReceivedSize(long receivedSize) {
        this.receivedSize = receivedSize;
    }
    public long getReceivedSize() {
        return this.receivedSize;
    }

    public java.lang.String contentSha256;
    public void setContentSha256(java.lang.String contentSha256) {
        this.contentSha256 = contentSha256;
    }
    public java.lang.String getContentSha256() {
        return this.contentSha256;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String failureCode;
    public void setFailureCode(java.lang.String failureCode) {
        this.failureCode = failureCode;
    }
    public java.lang.String getFailureCode() {
        return this.failureCode;
    }

    public java.lang.String expiresAt;
    public void setExpiresAt(java.lang.String expiresAt) {
        this.expiresAt = expiresAt;
    }
    public java.lang.String getExpiresAt() {
        return this.expiresAt;
    }

    public java.lang.String createAt;
    public void setCreateAt(java.lang.String createAt) {
        this.createAt = createAt;
    }
    public java.lang.String getCreateAt() {
        return this.createAt;
    }

    public java.lang.String updateAt;
    public void setUpdateAt(java.lang.String updateAt) {
        this.updateAt = updateAt;
    }
    public java.lang.String getUpdateAt() {
        return this.updateAt;
    }

    public java.util.List receivedChunks;
    public void setReceivedChunks(java.util.List receivedChunks) {
        this.receivedChunks = receivedChunks;
    }
    public java.util.List getReceivedChunks() {
        return this.receivedChunks;
    }

}
