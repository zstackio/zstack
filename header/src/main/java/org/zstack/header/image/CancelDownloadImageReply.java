package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2019/7/13.
 */
public class CancelDownloadImageReply extends MessageReply {
    private String completedUploadInstallPath;

    public String getCompletedUploadInstallPath() {
        return completedUploadInstallPath;
    }

    public void setCompletedUploadInstallPath(String completedUploadInstallPath) {
        this.completedUploadInstallPath = completedUploadInstallPath;
    }
}
