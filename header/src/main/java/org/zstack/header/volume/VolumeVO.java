package org.zstack.header.volume;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = VolumeEO.class)
@AutoDeleteTag
public class VolumeVO extends VolumeAO {
}
