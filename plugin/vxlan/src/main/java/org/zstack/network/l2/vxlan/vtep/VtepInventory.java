package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.query.Queryable;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 06/03/2017.
 */
public class VtepInventory {
    private String uuid;

    private String hostUuid;

    private String vtepCidr;

    private String vtepIp;

    private Integer port;

    private String physicalInterface;

    @Queryable(mappingClass = VtepL2NetworkRefInventory.class,
        joinColumn = @JoinColumn(name = "vtepUuid", referencedColumnName = "l2NetworkUuid"))
    private List<String> attachedNetworkUuids;

    public VtepInventory() {
    }

    protected VtepInventory(VtepVO vo) {
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setVtepCidr(vo.getVtepCidr());
        this.setVtepIp(vo.getVtepIp());
        this.setPort(vo.getPort());
        this.setPhysicalInterface(vo.getPhysicalInterface());
        this.attachedNetworkUuids = new ArrayList<String>(vo.getAttachedNetworkRefs().size());
        for (VtepL2NetworkRefVO ref : vo.getAttachedNetworkRefs()) {
            this.attachedNetworkUuids.add(ref.getL2NetworkUuid());
        }
    }

    public static VtepInventory valueOf(VtepVO vo) {
        return new VtepInventory(vo);
    }

    public static List<VtepInventory> valueOf(Collection<VtepVO> vos) {
        List<VtepInventory> invs = new ArrayList<>(vos.size());
        for (VtepVO vo: vos) {
            invs.add(VtepInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public List<String> getAttachedNetworkUuids() {
        return attachedNetworkUuids;
    }

    public void setAttachedNetworkUuids(List<String> attachedNetworkUuids) {
        this.attachedNetworkUuids = attachedNetworkUuids;
    }
}
