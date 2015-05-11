package org.zstack.header.simulator.storage.backup;

import org.zstack.header.storage.backup.APIAddBackupStorageMsg;


public class APIAddSimulatorBackupStorageMsg extends APIAddBackupStorageMsg {
	private long totalCapacity;
	private long availableCapacity;
	
	public APIAddSimulatorBackupStorageMsg() {
	}

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalCapacity() {
    	return totalCapacity;
    }
	public void setTotalCapacity(long totalCapacity) {
    	this.totalCapacity = totalCapacity;
    }
}
