package org.zstack.sdk;



public class CheckCephHealthResult {
    public boolean health;
    public void setHealth(boolean health) {
        this.health = health;
    }
    public boolean getHealth() {
        return this.health;
    }

    public java.util.List infos;
    public void setInfos(java.util.List infos) {
        this.infos = infos;
    }
    public java.util.List getInfos() {
        return this.infos;
    }

}
