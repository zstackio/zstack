package org.zstack.header.cluster;

import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ClusterEO.class)
public class ClusterVO extends ClusterAO {
}
