package org.zstack.header.zone;

import org.zstack.header.hierarchy.EntityHierarchy;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ZoneEO.class)
@BaseResource
@EntityHierarchy(parent = Object.class, myField = "", targetField = "")
public class ZoneVO extends ZoneAO {
}
