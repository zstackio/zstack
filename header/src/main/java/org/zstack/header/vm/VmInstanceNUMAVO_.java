package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceNUMAVO.class)
public class VmInstanceNUMAVO_ {
    public static volatile SingularAttribute<VmInstanceNUMAVO, Long> id;
    public static volatile SingularAttribute<VmInstanceNUMAVO, String> uuid;
    public static volatile SingularAttribute<VmInstanceNUMAVO, Integer> vNodeID;
    public static volatile SingularAttribute<VmInstanceNUMAVO, String> vNodeCPUs;
    public static volatile SingularAttribute<VmInstanceNUMAVO, Long> vNodeMemeSize;
    public static volatile SingularAttribute<VmInstanceNUMAVO, String> vNodeDistance;
    public static volatile SingularAttribute<VmInstanceNUMAVO, Integer> pNodeID;

}
