package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/30.
 */
public class ModifyVipAttributesStruct {
    private String serviceProvider;
    private boolean isServiceProvider;
    private String useFor;
    private boolean isUserFor;
    private String peerL3NetworkUuid;
    private boolean isPeerL3NetworkUuid;

    public void clearServiceProvider() {
        serviceProvider = null;
        isServiceProvider = false;
    }

    public void clearUseFor() {
        useFor = null;
        isUserFor = false;
    }

    public void clearPeerL3NetworkUuid() {
        peerL3NetworkUuid = null;
        isPeerL3NetworkUuid = false;
    }

    public boolean isServiceProvider() {
        return isServiceProvider;
    }

    public boolean isUserFor() {
        return isUserFor;
    }

    public boolean isPeerL3NetworkUuid() {
        return isPeerL3NetworkUuid;
    }


    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
        isServiceProvider = true;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
        isUserFor = true;
    }

    public String getPeerL3NetworkUuid() {
        return peerL3NetworkUuid;
    }

    public void setPeerL3NetworkUuid(String peerL3NetworkUuid) {
        this.peerL3NetworkUuid = peerL3NetworkUuid;
        isPeerL3NetworkUuid = true;
    }
}
