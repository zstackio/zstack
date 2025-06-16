package org.zstack.header.network.sdncontroller;

import org.zstack.header.host.HostEO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;

/**
 * Created by shixin.ruan on 09/30/2019.
 */
@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass = SdnControllerVO.class, foreignVOJoinColumn = "sdnControllerUuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = SdnControllerVO.class, joinColumn = "sdnControllerUuid"),
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SdnControllerVO.class, myField = "sdnControllerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostEO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class SdnControllerHostRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = SdnControllerVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String sdnControllerUuid;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String vSwitchType;

    @Column
    private String vtepIp;

    @Column
    private String netmask;

    @Column
    private String nicPciAddresses;

    @Column
    private String nicDrivers;

    @Column
    private String bondMode;

    @Column
    private String lacpMode;


    public SdnControllerHostRefVO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getvSwitchType() {
        return vSwitchType;
    }

    public void setvSwitchType(String vSwitchType) {
        this.vSwitchType = vSwitchType;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public String getNicPciAddresses() {
        return nicPciAddresses;
    }

    public void setNicPciAddresses(String nicPciAddresses) {
        this.nicPciAddresses = nicPciAddresses;
    }

    public String getNicDrivers() {
        return nicDrivers;
    }

    public void setNicDrivers(String nicDrivers) {
        this.nicDrivers = nicDrivers;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getBondMode() {
        return bondMode;
    }

    public void setBondMode(String bondMode) {
        this.bondMode = bondMode;
    }

    public String getLacpMode() {
        return lacpMode;
    }

    public void setLacpMode(String lacpMode) {
        this.lacpMode = lacpMode;
    }

    public static SdnControllerHostRefVO fromOther(SdnControllerHostRefVO vo) {
        SdnControllerHostRefVO newRef = new SdnControllerHostRefVO();
        newRef.sdnControllerUuid = vo.getSdnControllerUuid();
        newRef.hostUuid = vo.getHostUuid();
        newRef.vSwitchType = vo.getvSwitchType();
        newRef.vtepIp = vo.getVtepIp();
        newRef.netmask = vo.getNetmask();
        newRef.bondMode = vo.getBondMode();
        newRef.nicPciAddresses = vo.nicPciAddresses;
        newRef.nicDrivers = vo.getNicDrivers();
        newRef.lacpMode = vo.getLacpMode();

        return newRef;
    }
}
