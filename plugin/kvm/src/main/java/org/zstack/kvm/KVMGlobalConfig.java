package org.zstack.kvm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.zone.ZoneVO;

/**
 */
@GlobalConfigDefinition
public class KVMGlobalConfig {
    public static final String CATEGORY = "kvm";

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_MIGRATION_QUANTITY = new GlobalConfig(CATEGORY, "vm.migrationQuantity");
    @GlobalConfigValidation(numberGreaterThan = -1)
    public static GlobalConfig RESERVED_CPU_CAPACITY = new GlobalConfig(CATEGORY, "reservedCpu");
    @GlobalConfigValidation
    @BindResourceConfig({HostVO.class, ClusterVO.class, ZoneVO.class})
    public static GlobalConfig RESERVED_MEMORY_CAPACITY = new GlobalConfig(CATEGORY, "reservedMemory");
    @GlobalConfigValidation(inNumberRange = {0, 24})
    public static GlobalConfig MAX_DATA_VOLUME_NUM = new GlobalConfig(CATEGORY, "dataVolume.maxNum");
    @GlobalConfigValidation(numberGreaterThan = 2)
    public static GlobalConfig HOST_SYNC_LEVEL = new GlobalConfig(CATEGORY, "host.syncLevel");
    @GlobalConfigValidation(numberGreaterThan = 2)
    public static GlobalConfig HOST_SNAPSHOT_SYNC_LEVEL = new GlobalConfig(CATEGORY, "host.snapshot.syncLevel");
    @GlobalConfigValidation(inNumberRange = {1, 10})
    public static GlobalConfig VM_CREATE_CONCURRENCY = new GlobalConfig(CATEGORY, "vm.createConcurrency");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_ALIYUN = new GlobalConfig(CATEGORY, "host.DNSCheckAliyun");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_163 = new GlobalConfig(CATEGORY, "host.DNSCheck163");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_LIST = new GlobalConfig(CATEGORY, "host.DNSCheckList");
    @GlobalConfigValidation
    public static GlobalConfig ALLOW_LIVE_SNAPSHOT_ON_REDHAT = new GlobalConfig(CATEGORY, "redhat.liveSnapshotOn");
    @GlobalConfigValidation(validValues = {"none", "writethrough", "writeback"})
    public static GlobalConfig LIBVIRT_CACHE_MODE = new GlobalConfig(CATEGORY, "vm.cacheMode");
    @GlobalConfigValidation(validValues = {"none", "host-model", "host-passthrough", "Haswell", "Haswell-noTSX", "Broadwell", "Broadwell-noTSX", "SandyBridge", "IvyBridge", "Conroe", "Penryn", "Nehalem", "Westmere", "Opteron_G1", "Opteron_G2", "Opteron_G3", "Opteron_G4"})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig NESTED_VIRTUALIZATION = new GlobalConfig(CATEGORY, "vm.cpuMode");
    @GlobalConfigValidation
    public static GlobalConfig VM_SYNC_ON_HOST_PING = new GlobalConfig(CATEGORY, "vmSyncOnHostPing");
    @GlobalConfigValidation
    public static GlobalConfig CHECK_HOST_CPU_MODEL_NAME = new GlobalConfig(CATEGORY, "checkHostCpuModelName");
    @GlobalConfigValidation
    public static GlobalConfig KVM_IGNORE_MSRS = new GlobalConfig(CATEGORY, "ignoreMsrs");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({ClusterVO.class})
    public static GlobalConfig AUTO_VM_NIC_MULTIQUEUE = new GlobalConfig(CATEGORY, "auto.set.vm.nic.multiqueue");
    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_AUTO_CONVERGE = new GlobalConfig(CATEGORY, "migrate.autoConverge");
    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_XBZRLE = new GlobalConfig(CATEGORY, "migrate.xbzrle");
    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 3600)
    public static GlobalConfig TEST_SSH_PORT_ON_OPEN_TIMEOUT = new GlobalConfig(CATEGORY, "testSshPortOpenTimeout");
    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 300)
    public static GlobalConfig TEST_SSH_PORT_ON_CONNECT_TIMEOUT = new GlobalConfig(CATEGORY, "testSshPortOnConnectTimeout");
    @GlobalConfigValidation
    public static GlobalConfig RESTART_AGENT_IF_FAKE_DEAD = new GlobalConfig(CATEGORY, "restartagentwhenfakedead");

    @GlobalConfigValidation
    public static GlobalConfig KVMAGENT_ALLOW_PORTS_LIST = new GlobalConfig(CATEGORY, "kvmagent.allow.ports");

    @GlobalConfigValidation
    public static GlobalConfig ENABLE_HOST_TCP_CONNECTION_CHECK = new GlobalConfig(CATEGORY, "enable.host.tcp.connection.check");
    @GlobalConfigValidation
    public static GlobalConfig HOST_CONNECTION_CHECK_INTERVAL = new GlobalConfig(CATEGORY, "host.connection.check.interval");
    @GlobalConfigValidation
    public static GlobalConfig CONNECTION_SERVER_UPDATE_INTERVAL = new GlobalConfig(CATEGORY, "connection.server.update.interval");
}
