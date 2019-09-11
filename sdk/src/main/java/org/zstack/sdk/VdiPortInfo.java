package org.zstack.sdk;

/**
 * author:kaicai.hu
 * Date:2019/9/16
 */
public class VdiPortInfo  {

    public java.lang.Integer vncPort;
    public void setVncPort(java.lang.Integer vncPort) {
        this.vncPort = vncPort;
    }
    public java.lang.Integer getVncPort() {
        return this.vncPort;
    }

    public java.lang.Integer spicePort;
    public void setSpicePort(java.lang.Integer spicePort) {
        this.spicePort = spicePort;
    }
    public java.lang.Integer getSpicePort() {
        return this.spicePort;
    }

    public java.lang.Integer spiceTlsPort;
    public void setSpiceTlsPort(java.lang.Integer spiceTlsPort) {
        this.spiceTlsPort = spiceTlsPort;
    }
    public java.lang.Integer getSpiceTlsPort() {
        return this.spiceTlsPort;
    }

    public boolean spiceTls;
    public void setSpiceTls(boolean spiceTls) {
        this.spiceTls = spiceTls;
    }
    public boolean getSpiceTls() {
        return this.spiceTls;
    }

}
