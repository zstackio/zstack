package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

public class GetImageDownloadProgressReply extends MessageReply {
    private boolean completed;
    private long downloaded;

    private long size;
    private long actualSize;
    private String installPath;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
