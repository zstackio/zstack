package org.zstack.network.service.lb;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@Entity
@Table
public class LoadBalancerListenerVmNicRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    private String listenerUuid;

    @Column
    private String vmNicUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerVmNicStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public LoadBalancerVmNicStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerVmNicStatus status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
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
