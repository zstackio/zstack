package org.zstack.header.configuration;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = DiskOfferingEO.class)
public class DiskOfferingVO extends DiskOfferingAO {
}
