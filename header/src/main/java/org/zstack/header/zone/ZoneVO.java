package org.zstack.header.zone;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ZoneEO.class)
public class ZoneVO extends ZoneAO {
}
