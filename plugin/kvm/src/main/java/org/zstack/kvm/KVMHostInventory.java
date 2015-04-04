package org.zstack.kvm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.rest.APINoSee;

@PythonClassInventory
public class KVMHostInventory extends HostInventory {
    /**
     * @ignore
     */
    @APINoSee
    private String username;
    /**
     * @ignore
     */
    @APINoSee
    private String password;
    
    protected KVMHostInventory(KVMHostVO vo) {
        super(vo);
        this.setUsername(username);
        this.setPassword(password);
    }
    
    public static KVMHostInventory valueOf(KVMHostVO vo) {
        KVMHostInventory inv = new KVMHostInventory(vo);
        return inv;
    }
    
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
