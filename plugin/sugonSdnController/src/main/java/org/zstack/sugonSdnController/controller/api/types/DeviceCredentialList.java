//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DeviceCredentialList extends ApiPropertyBase {
    List<DeviceCredential> device_credential;
    public DeviceCredentialList() {
    }
    public DeviceCredentialList(List<DeviceCredential> device_credential) {
        this.device_credential = device_credential;
    }
    
    public List<DeviceCredential> getDeviceCredential() {
        return device_credential;
    }
    
    
    public void addDeviceCredential(DeviceCredential obj) {
        if (device_credential == null) {
            device_credential = new ArrayList<DeviceCredential>();
        }
        device_credential.add(obj);
    }
    public void clearDeviceCredential() {
        device_credential = null;
    }
    
    
    public void addDeviceCredential(UserCredentials credential, String vendor, String device_family) {
        if (device_credential == null) {
            device_credential = new ArrayList<DeviceCredential>();
        }
        device_credential.add(new DeviceCredential(credential, vendor, device_family));
    }
    
}
