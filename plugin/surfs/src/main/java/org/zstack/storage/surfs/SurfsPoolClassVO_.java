package org.zstack.storage.surfs;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.storage.surfs.SurfsPoolClassVO;

@StaticMetamodel(SurfsPoolClassVO.class)
public class SurfsPoolClassVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SurfsPoolClassVO, String> Uuid;
    public static volatile SingularAttribute<SurfsPoolClassVO, String> fsid;
    public static volatile SingularAttribute<SurfsPoolClassVO, String> clsname;
    public static volatile SingularAttribute<SurfsPoolClassVO, String> clsdisplayname;
    public static volatile SingularAttribute<SurfsPoolClassVO, Timestamp> createDate;
    public static volatile SingularAttribute<SurfsPoolClassVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SurfsPoolClassVO, Long> totalCapacity;
    public static volatile SingularAttribute<SurfsPoolClassVO, Long> availableCapacity;
    public static volatile SingularAttribute<SurfsPoolClassVO, Boolean> isrootcls; 
    public static volatile SingularAttribute<SurfsPoolClassVO, Boolean> isactive;
}