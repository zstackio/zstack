package org.zstack.sdk;



public class HostNetworkInterfaceInventory  {

    public java.lang.String interfaceName;
    public void setInterfaceName(java.lang.String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public java.lang.String getInterfaceName() {
        return this.interfaceName;
    }

    public java.lang.Long speed;
    public void setSpeed(java.lang.Long speed) {
        this.speed = speed;
    }
    public java.lang.Long getSpeed() {
        return this.speed;
    }

    public boolean slaveActive;
    public void setSlaveActive(boolean slaveActive) {
        this.slaveActive = slaveActive;
    }
    public boolean getSlaveActive() {
        return this.slaveActive;
    }

    public boolean carrierActive;
    public void setCarrierActive(boolean carrierActive) {
        this.carrierActive = carrierActive;
    }
    public boolean getCarrierActive() {
        return this.carrierActive;
    }

}
