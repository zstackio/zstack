package org.zstack.header.volume;

import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
public class VolumeVO extends VolumeAO {
}
