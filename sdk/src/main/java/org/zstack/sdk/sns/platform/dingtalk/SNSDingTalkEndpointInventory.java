package org.zstack.sdk.sns.platform.dingtalk;



public class SNSDingTalkEndpointInventory extends org.zstack.sdk.sns.SNSApplicationEndpointInventory {

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public boolean atAll;
    public void setAtAll(boolean atAll) {
        this.atAll = atAll;
    }
    public boolean getAtAll() {
        return this.atAll;
    }

    public java.util.List atPersons;
    public void setAtPersons(java.util.List atPersons) {
        this.atPersons = atPersons;
    }
    public java.util.List getAtPersons() {
        return this.atPersons;
    }

}
