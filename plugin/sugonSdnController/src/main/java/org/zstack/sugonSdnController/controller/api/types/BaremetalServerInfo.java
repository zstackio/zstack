//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BaremetalServerInfo extends ApiPropertyBase {
    String network_interface;
    String driver;
    BaremetalProperties properties;
    DriverInfo driver_info;
    String name;
    public BaremetalServerInfo() {
    }
    public BaremetalServerInfo(String network_interface, String driver, BaremetalProperties properties, DriverInfo driver_info, String name) {
        this.network_interface = network_interface;
        this.driver = driver;
        this.properties = properties;
        this.driver_info = driver_info;
        this.name = name;
    }
    public BaremetalServerInfo(String network_interface) {
        this(network_interface, null, null, null, null);    }
    public BaremetalServerInfo(String network_interface, String driver) {
        this(network_interface, driver, null, null, null);    }
    public BaremetalServerInfo(String network_interface, String driver, BaremetalProperties properties) {
        this(network_interface, driver, properties, null, null);    }
    public BaremetalServerInfo(String network_interface, String driver, BaremetalProperties properties, DriverInfo driver_info) {
        this(network_interface, driver, properties, driver_info, null);    }
    
    public String getNetworkInterface() {
        return network_interface;
    }
    
    public void setNetworkInterface(String network_interface) {
        this.network_interface = network_interface;
    }
    
    
    public String getDriver() {
        return driver;
    }
    
    public void setDriver(String driver) {
        this.driver = driver;
    }
    
    
    public BaremetalProperties getProperties() {
        return properties;
    }
    
    public void setProperties(BaremetalProperties properties) {
        this.properties = properties;
    }
    
    
    public DriverInfo getDriverInfo() {
        return driver_info;
    }
    
    public void setDriverInfo(DriverInfo driver_info) {
        this.driver_info = driver_info;
    }
    
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
}
