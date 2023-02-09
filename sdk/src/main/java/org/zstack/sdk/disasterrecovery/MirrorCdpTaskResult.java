package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.ErrorCode;

public class MirrorCdpTaskResult  {

    public java.lang.String mirrorCdpTaskUuid;
    public void setMirrorCdpTaskUuid(java.lang.String mirrorCdpTaskUuid) {
        this.mirrorCdpTaskUuid = mirrorCdpTaskUuid;
    }
    public java.lang.String getMirrorCdpTaskUuid() {
        return this.mirrorCdpTaskUuid;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}
