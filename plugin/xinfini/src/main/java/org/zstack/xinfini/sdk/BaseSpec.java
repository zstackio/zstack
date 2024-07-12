package org.zstack.xinfini.sdk;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:41 2024/5/28
 */
public class BaseSpec {
    private int id;
    private String etag;
    private String name;
    private String description;
    private String uuid;
    private String creator;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String creationFinish;
    private String deletionBegin;
    private String deletionFinish;
    private String creationTimeoutAt;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
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

    public String getCreationFinish() {
        return creationFinish;
    }

    public void setCreationFinish(String creationFinish) {
        this.creationFinish = creationFinish;
    }

    public String getDeletionBegin() {
        return deletionBegin;
    }

    public void setDeletionBegin(String deletionBegin) {
        this.deletionBegin = deletionBegin;
    }

    public String getDeletionFinish() {
        return deletionFinish;
    }

    public void setDeletionFinish(String deletionFinish) {
        this.deletionFinish = deletionFinish;
    }
}
