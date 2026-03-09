package org.zstack.header.allocator;


import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostAllocatedCpuVO.class)
public class HostAllocatedCpuVO_ {
    public static volatile SingularAttribute<HostAllocatedCpuVO, Long> id;
    public static volatile SingularAttribute<HostAllocatedCpuVO, String> hostUuid;
    public static volatile SingularAttribute<HostAllocatedCpuVO, Integer> allocatedCPU;
}
