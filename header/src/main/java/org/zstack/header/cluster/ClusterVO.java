package org.zstack.header.cluster;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ClusterEO.class)
@BaseResource
public class ClusterVO extends ClusterAO {
}
