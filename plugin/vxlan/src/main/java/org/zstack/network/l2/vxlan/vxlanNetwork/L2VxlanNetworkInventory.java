package org.zstack.network.l2.vxlan.vxlanNetwork;


import org.zstack.header.network.l2.L2NetworkInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory
 * @category
 * @example {
 * "org.zstack.header.network.l2.APICreateL2VxlanNetworkEvent": {
 * "inventory": {
 * "vni": 10,
 * "uuid": "14a01b0978684b2ea6e5a355c7c7fd73",
 * "name": "TestL2VxlanNetwork",
 * "description": "Test",
 * "zoneUuid": "c74f8ff8a4c5456b852713b82c034074",
 * "physicalInterface": "eth0.1100",
 * "type": "L2VxlanNetwork",
 * "poolUuid": "",
 * "createDate": "May 4, 2014 4:31:47 PM",
 * "lastOpDate": "May 4, 2014 4:31:47 PM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
public class L2VxlanNetworkInventory extends L2NetworkInventory {
    /**
     * @desc vlan id
     * @choices [0, 16777215]
     */
    private Integer vni;

    private String vtepCidr;

    private String poolUuid;

    public L2VxlanNetworkInventory() {
    }

    protected L2VxlanNetworkInventory (VxlanNetworkVO vo) {
        super(vo);
        this.setVni(vo.getVni());
    }

    public static L2VxlanNetworkInventory valueOf(VxlanNetworkVO vo) {
        return new L2VxlanNetworkInventory(vo);
    }

    public static List<L2VxlanNetworkInventory> valueOf1(Collection<VxlanNetworkVO> vos) {
        List<L2VxlanNetworkInventory> invs = new ArrayList<L2VxlanNetworkInventory>(vos.size());
        for (VxlanNetworkVO vo : vos) {
            invs.add(new L2VxlanNetworkInventory(vo));
        }
        return invs;
    }

    public int getVni() {
        return vni;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }
}
