//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BaremetalProperties extends ApiPropertyBase {
    Integer memory_mb;
    String cpu_arch;
    Integer local_gb;
    Integer cpus;
    String capabilities;
    public BaremetalProperties() {
    }
    public BaremetalProperties(Integer memory_mb, String cpu_arch, Integer local_gb, Integer cpus, String capabilities) {
        this.memory_mb = memory_mb;
        this.cpu_arch = cpu_arch;
        this.local_gb = local_gb;
        this.cpus = cpus;
        this.capabilities = capabilities;
    }
    public BaremetalProperties(Integer memory_mb) {
        this(memory_mb, null, null, null, null);    }
    public BaremetalProperties(Integer memory_mb, String cpu_arch) {
        this(memory_mb, cpu_arch, null, null, null);    }
    public BaremetalProperties(Integer memory_mb, String cpu_arch, Integer local_gb) {
        this(memory_mb, cpu_arch, local_gb, null, null);    }
    public BaremetalProperties(Integer memory_mb, String cpu_arch, Integer local_gb, Integer cpus) {
        this(memory_mb, cpu_arch, local_gb, cpus, null);    }
    
    public Integer getMemoryMb() {
        return memory_mb;
    }
    
    public void setMemoryMb(Integer memory_mb) {
        this.memory_mb = memory_mb;
    }
    
    
    public String getCpuArch() {
        return cpu_arch;
    }
    
    public void setCpuArch(String cpu_arch) {
        this.cpu_arch = cpu_arch;
    }
    
    
    public Integer getLocalGb() {
        return local_gb;
    }
    
    public void setLocalGb(Integer local_gb) {
        this.local_gb = local_gb;
    }
    
    
    public Integer getCpus() {
        return cpus;
    }
    
    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }
    
    
    public String getCapabilities() {
        return capabilities;
    }
    
    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }
    
}
