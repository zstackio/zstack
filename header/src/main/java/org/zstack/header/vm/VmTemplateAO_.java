package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmTemplateAO.class)
public class VmTemplateAO_ {
    public static volatile SingularAttribute<VmTemplateAO, String> uuid;
    public static volatile SingularAttribute<VmTemplateAO, String> name;
    public static volatile SingularAttribute<VmTemplateAO, String> instanceOfferingUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> imageUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> defaultL3NetworkUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> l3NetworkUuids;
    public static volatile SingularAttribute<VmTemplateAO, String> type;
    public static volatile SingularAttribute<VmTemplateAO, String> zoneUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> clusterUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> hostUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> rootDiskOfferingUuid;
    public static volatile SingularAttribute<VmTemplateAO, String> dataDiskOfferingUuids;
    public static volatile SingularAttribute<VmTemplateAO, Integer> cpuNum;
    public static volatile SingularAttribute<VmTemplateAO, Long> memorySize;
    public static volatile SingularAttribute<VmTemplateAO, String> primaryStorageUuidForRootVolume;
    public static volatile SingularAttribute<VmTemplateAO, String> primaryStorageUuidForDataVolume;
    public static volatile SingularAttribute<VmTemplateAO, String> rootVolumeSystemTags;
    public static volatile SingularAttribute<VmTemplateAO, String> dataVolumeSystemTags;
    public static volatile SingularAttribute<VmTemplateAO, String> description;
    public static volatile SingularAttribute<VmTemplateAO, String> strategy;
    public static volatile SingularAttribute<VmTemplateAO, String> systemTags;
    public static volatile SingularAttribute<VmTemplateAO, String> tagUuids;
}
