package org.zstack.header.zone;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ZoneEO.class)
@BaseResource
public class ZoneVO extends ZoneAO {
}
