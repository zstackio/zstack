package org.zstack.network.service.lb;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
public class LoadBalancerVmNicRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    private String loadBalancerUuid;

    @Column
    private String vmNicUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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
}
