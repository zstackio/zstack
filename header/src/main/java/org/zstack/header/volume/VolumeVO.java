package org.zstack.header.volume;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
public class VolumeVO extends VolumeAO {

}
