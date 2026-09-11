package org.zstack.sdk.network.zns.protocol;



public class Migration  {

    public int from;
    public void setFrom(int from) {
        this.from = from;
    }
    public int getFrom() {
        return this.from;
    }

    public int to;
    public void setTo(int to) {
        this.to = to;
    }
    public int getTo() {
        return this.to;
    }

    public java.lang.String migratorId;
    public void setMigratorId(java.lang.String migratorId) {
        this.migratorId = migratorId;
    }
    public java.lang.String getMigratorId() {
        return this.migratorId;
    }

    public boolean rollbackSupported;
    public void setRollbackSupported(boolean rollbackSupported) {
        this.rollbackSupported = rollbackSupported;
    }
    public boolean getRollbackSupported() {
        return this.rollbackSupported;
    }

}
