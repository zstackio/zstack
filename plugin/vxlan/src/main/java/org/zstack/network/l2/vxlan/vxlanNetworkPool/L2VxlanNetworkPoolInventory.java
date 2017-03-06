package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.L2NetworkInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory
 * @category
 * @example {
 * "org.zstack.header.network.l2.APICreateL2VxlanNetworkPoolEvent": {
 * "inventory": {
 * "startVni": 10,
 * "endVni": 100,
 * "uuid": "14a01b0978684b2ea6e5a355c7c7fd73",
 * "name": "TestL2VxlanNetworkPool",
 * "description": "Test",
 * "zoneUuid": "c74f8ff8a4c5456b852713b82c034074",
 * "physicalInterface": "eth0.1100",
 * "vtepCidr": "172.20.0.0/24",
 * "type": "L2VxlanNetwork",
 * "createDate": "May 4, 2014 4:31:47 PM",
 * "lastOpDate": "May 4, 2014 4:31:47 PM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 1.10.0
 */
public class L2VxlanNetworkPoolInventory extends L2NetworkInventory {
    /**
     * @desc vni start
     * @choices [1, 16777215]
     */
    private Integer startVni;

    /**
     * @desc vni end
     * @choices [1, 16777215]
     */
    private Integer endVni;

    private String vtepCidr;

    public L2VxlanNetworkPoolInventory() {
    }

    protected L2VxlanNetworkPoolInventory(VxlanNetworkPoolVO vo) {
        super(vo);
        this.setStartVni(vo.getStartVni());
        this.setEndVni(vo.getEndVni());
        this.setVtepCidr(vo.getVtepCidr());
    }

    public static L2VxlanNetworkPoolInventory valueOf(VxlanNetworkPoolVO vo) {
        return new L2VxlanNetworkPoolInventory();
    }

    public static List<L2VxlanNetworkPoolInventory> valueOf1(Collection<VxlanNetworkPoolVO> vos) {
        List<L2VxlanNetworkPoolInventory> invs = new ArrayList<L2VxlanNetworkPoolInventory>(vos.size());
        for (VxlanNetworkPoolVO vo : vos) {
            invs.add(new L2VxlanNetworkPoolInventory(vo));
        }
        return invs;
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

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }

}
