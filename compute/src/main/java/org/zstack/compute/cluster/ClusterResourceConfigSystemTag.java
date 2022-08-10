package org.zstack.compute.cluster;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class ClusterResourceConfigSystemTag {
    public static final String KVM_VM_CPU_MODE_TOKEN = "kvmVmCpuMode";
    public static PatternedSystemTag KVM_VM_CPU_MODE = new PatternedSystemTag(String.format("resourceConfig::kvm::vm.cpuMode::{%s}", KVM_VM_CPU_MODE_TOKEN), ClusterVO.class);
}
