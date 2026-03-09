package org.zstack.header.network.l3;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table
@EO(EOClazz = IpRangeEO.class, needView = false)
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@AutoDeleteTag
public class NormalIpRangeVO extends IpRangeVO {
}
