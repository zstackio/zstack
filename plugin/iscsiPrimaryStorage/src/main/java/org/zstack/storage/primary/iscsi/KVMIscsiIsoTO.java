package org.zstack.storage.primary.iscsi;

import org.zstack.kvm.KVMAgentCommands.IsoTO;

/**
 * Created by frank on 6/7/2015.
 */
public class KVMIscsiIsoTO extends IsoTO {
    private String chapUsername;
    private String chapPassword;
    private String hostname;
    private int port;
    private String target;
    private int lun;

    public KVMIscsiIsoTO() {
    }

    public KVMIscsiIsoTO(KVMIscsiIsoTO other) {
        super(other);
        this.chapUsername = other.chapUsername;
        this.chapPassword = other.chapPassword;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostName) {
        this.hostname = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getLun() {
        return lun;
    }

    public void setLun(int lun) {
        this.lun = lun;
    }

    public KVMIscsiIsoTO(IsoTO other) {
        super(other);
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
