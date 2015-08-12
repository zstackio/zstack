package org.zstack.header.cluster;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ClusterEO.class)
@AutoDeleteTag
public class ClusterVO extends ClusterAO {
}
