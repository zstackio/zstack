package org.zstack.header.cluster;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@EO(EOClazz = ClusterEO.class)
@AutoDeleteTag
public class ClusterVO extends ClusterAO {
}
