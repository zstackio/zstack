package org.zstack.utils.zsha2;

/**
 * @author hanyu.liang
 * @date 2023/11/6 17:02
 */
public class ZSha2StatusJsonInfo {
    private boolean ownsVip;
    private boolean peerReachable;
    private boolean gwReachable;
    private boolean vipReachable;
    private String dbStatus;
    private String mnStatus;
    private String timeToSyncDB;
    private boolean slaveIoRunning;
    private boolean slaveSqlRuning;

    public boolean isOwnsVip() {
        return ownsVip;
    }

    public void setOwnsVip(boolean ownsVip) {
        this.ownsVip = ownsVip;
    }

    public boolean isPeerReachable() {
        return peerReachable;
    }

    public void setPeerReachable(boolean peerReachable) {
        this.peerReachable = peerReachable;
    }

    public boolean isGwReachable() {
        return gwReachable;
    }

    public void setGwReachable(boolean gwReachable) {
        this.gwReachable = gwReachable;
    }

    public boolean isVipReachable() {
        return vipReachable;
    }

    public void setVipReachable(boolean vipReachable) {
        this.vipReachable = vipReachable;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public void setDbStatus(String dbStatus) {
        this.dbStatus = dbStatus;
    }

    public String getMnStatus() {
        return mnStatus;
    }

    public void setMnStatus(String mnStatus) {
        this.mnStatus = mnStatus;
    }

    public String getTimeToSyncDB() {
        return timeToSyncDB;
    }

    public void setTimeToSyncDB(String timeToSyncDB) {
        this.timeToSyncDB = timeToSyncDB;
    }

    public boolean isSlaveIoRunning() {
        return slaveIoRunning;
    }

    public void setSlaveIoRunning(boolean slaveIoRunning) {
        this.slaveIoRunning = slaveIoRunning;
    }

    public boolean isSlaveSqlRuning() {
        return slaveSqlRuning;
    }

    public void setSlaveSqlRuning(boolean slaveSqlRuning) {
        this.slaveSqlRuning = slaveSqlRuning;
    }
}
