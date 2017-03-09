package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by weiwang on 08/03/2017.
 */
@Entity
@Table
@AutoDeleteTag
public class VniRangeVO {
    @Column
    private String uuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Integer startVni;

    @Column
    private Integer endVni;

    @Column
    private String l2NetworkUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public Integer getStartVni() {
        return startVni;
    }

    public void setStartVni(Integer startVni) {
        this.startVni = startVni;
    }

    public Integer getEndVni() {
        return endVni;
    }

    public void setEndVni(Integer endVni) {
        this.endVni = endVni;
    }

}
