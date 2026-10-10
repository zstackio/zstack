package org.zstack.kvm.memory;

/**
 * Fixed public projection of the existing raw identity object.
 * uint64-like tokens remain decimal strings; generation labels remain opaque strings.
 */
public class MemoryVmIdentityInventory {
    private String uuid;
    private String hostBootId;
    /** @deprecated r12 VM-list compatibility; use instanceGeneration. */
    @Deprecated private String instance_generation;
    private String instanceGeneration;
    private String mmContextId;
    private String cgroupId;
    private String startTime;
    private String sampledMonotonicNs;
    private String cgroupInode;
    private String sequence;
    private String cgroupPath;
    private String poolGeneration;
    private String device;
    private Integer pid;

    public static MemoryVmIdentityInventory __example__() {
        MemoryVmIdentityInventory identity = new MemoryVmIdentityInventory();
        identity.uuid = "03d98313faa24670a0944b10da8d1ac2";
        identity.hostBootId = "8c6ce488-e264-4c1d-bcd9-763196d10a3e";
        identity.setInstanceGeneration("96a20bcfedbd7fd6759c775ebb82333555d51a623b68da55d4c5c1ed114e1da7");
        // start_time is the unsigned decimal /proc start-tick token, not a wall-clock timestamp.
        identity.startTime = "116424049";
        identity.cgroupInode = "239821";
        identity.cgroupPath = "/sys/fs/cgroup/machine.slice/machine-qemu\\x2d7\\x2d03d98313faa24670a0944b10da8d1ac2.scope/libvirt";
        // Generation tokens are decimal uint64 strings. This is the generation from the captured sample.
        identity.poolGeneration = "1051626769722038414";
        identity.device = "/dev/zram0";
        identity.pid = 3279904;
        return identity;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getHostBootId() { return hostBootId; }
    public void setHostBootId(String value) { hostBootId = value; }
    public String getInstanceGeneration() { return instanceGeneration; }
    public void setInstanceGeneration(String value) { instanceGeneration = value; instance_generation = value; }
    /** @deprecated r12 VM-list alias; use instanceGeneration. */
    @Deprecated public String getInstance_generation() { return instance_generation; }
    @Deprecated public void setInstance_generation(String value) { setInstanceGeneration(value); }
    public String getMmContextId() { return mmContextId; }
    public void setMmContextId(String value) { mmContextId = value; }
    public String getCgroupId() { return cgroupId; }
    public void setCgroupId(String value) { cgroupId = value; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String value) { startTime = value; }
    public String getSampledMonotonicNs() { return sampledMonotonicNs; }
    public void setSampledMonotonicNs(String value) { sampledMonotonicNs = value; }
    public String getCgroupInode() { return cgroupInode; }
    public void setCgroupInode(String value) { cgroupInode = value; }
    public String getSequence() { return sequence; }
    public void setSequence(String value) { sequence = value; }
    public String getCgroupPath() { return cgroupPath; }
    public void setCgroupPath(String value) { cgroupPath = value; }
    public String getPoolGeneration() { return poolGeneration; }
    public void setPoolGeneration(String value) { poolGeneration = value; }
    public String getDevice() { return device; }
    public void setDevice(String value) { device = value; }
    public Integer getPid() { return pid; }
    public void setPid(Integer value) { pid = value; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getHost_boot_id() { return hostBootId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getMm_context_id() { return mmContextId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getCgroup_id() { return cgroupId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getStart_time() { return startTime; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getSampled_monotonic_ns() { return sampledMonotonicNs; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getCgroup_inode() { return cgroupInode; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getCgroup_path() { return cgroupPath; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getPool_generation() { return poolGeneration; }
}
