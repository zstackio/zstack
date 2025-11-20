package org.zstack.sdk;

import org.zstack.sdk.NfvInstClusterStatus;
import org.zstack.sdk.NfvInstClusterStatus;

public class OvnControllerVmInstanceInventory extends org.zstack.sdk.NfvInstInventory {

    public NfvInstClusterStatus nbClusterStatus;
    public void setNbClusterStatus(NfvInstClusterStatus nbClusterStatus) {
        this.nbClusterStatus = nbClusterStatus;
    }
    public NfvInstClusterStatus getNbClusterStatus() {
        return this.nbClusterStatus;
    }

    public NfvInstClusterStatus sbClusterStatus;
    public void setSbClusterStatus(NfvInstClusterStatus sbClusterStatus) {
        this.sbClusterStatus = sbClusterStatus;
    }
    public NfvInstClusterStatus getSbClusterStatus() {
        return this.sbClusterStatus;
    }

}
