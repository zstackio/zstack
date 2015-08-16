package org.zstack.network.service.lb;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
@AutoDeleteTag
public class LoadBalancerListenerVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String loadBalancerUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private int instancePort;

    @Column
    private int loadBalancerPort;

    @Column
    private String protocol;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}
