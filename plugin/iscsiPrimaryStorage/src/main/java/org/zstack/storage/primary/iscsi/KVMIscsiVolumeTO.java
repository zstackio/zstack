package org.zstack.storage.primary.iscsi;

import org.zstack.kvm.KVMAgentCommands.VolumeTO;

/**
 * Created by frank on 4/21/2015.
 */
public class KVMIscsiVolumeTO extends VolumeTO {
    private String chapUsername;
    private String chapPassword;

    public KVMIscsiVolumeTO() {
    }

    public KVMIscsiVolumeTO(VolumeTO other) {
        super(other);
    }

    public KVMIscsiVolumeTO(KVMIscsiVolumeTO other) {
        super(other);
        this.chapUsername = other.chapUsername;
        this.chapPassword = other.chapPassword;
    }


    public String getChapUsername() {
        return chapUsername;
    }

    public void setChapUsername(String chapUsername) {
        this.chapUsername = chapUsername;
    }

    public String getChapPassword() {
        return chapPassword;
    }

    public void setChapPassword(String chapPassword) {
        this.chapPassword = chapPassword;
    }
}
