package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 16/10/26.
 */
public class SetRootPasswordReply extends MessageReply {
    private String rootPassword;
    private String qcowFile;

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public String getQcowFile() {
        return qcowFile;
    }

    public void setQcowFile(String qcowFile) {
        this.qcowFile = qcowFile;
    }
}
