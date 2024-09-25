package org.zstack.xinfini.sdk;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:43 2024/5/28
 */
public class BaseStatus {
    private int id;
    private String createdAt;
    private String updatedAt;
    private String creationTimeoutAt;
    private String deletedAt;
    private String etag;

    public String getCreationTimeoutAt() {
        return creationTimeoutAt;
    }

    public void setCreationTimeoutAt(String creationTimeoutAt) {
        this.creationTimeoutAt = creationTimeoutAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
