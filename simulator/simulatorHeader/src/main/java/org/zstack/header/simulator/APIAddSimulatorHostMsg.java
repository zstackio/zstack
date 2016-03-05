package org.zstack.header.simulator;

import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;

public class APIAddSimulatorHostMsg extends APIAddHostMsg {
	@APIParam
	private long memoryCapacity = 1000000000;
	@APIParam
	private long cpuCapacity = 1000000000;
	
	public long getMemoryCapacity() {
    	return memoryCapacity;
    }
	public void setMemoryCapacity(long memoryCapacity) {
    	this.memoryCapacity = memoryCapacity;
    }
	public long getCpuCapacity() {
    	return cpuCapacity;
    }
	public void setCpuCapacity(long cpuCapacity) {
    	this.cpuCapacity = cpuCapacity;
    }

}
