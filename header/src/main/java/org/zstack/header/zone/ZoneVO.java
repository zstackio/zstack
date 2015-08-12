package org.zstack.header.zone;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ZoneEO.class)
@AutoDeleteTag
public class ZoneVO extends ZoneAO {
}
