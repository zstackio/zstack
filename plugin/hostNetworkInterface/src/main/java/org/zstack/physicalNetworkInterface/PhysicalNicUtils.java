package org.zstack.physicalNetworkInterface;

import org.zstack.core.db.Q;
import org.zstack.physicalNetworkInterface.header.HostNetworkInterfaceVO;
import org.zstack.physicalNetworkInterface.header.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

public class PhysicalNicUtils {
    public static String getPciDeviceUuidFromPhysicalNicUuid(String physicalNicUuid) {
        HostNetworkInterfaceVO pfVO = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.uuid, physicalNicUuid)
                .find();

        if (pfVO == null) {
            return null;
        }

        String pciDeviceUuid = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, pfVO.getHostUuid())
                .eq(PciDeviceVO_.pciDeviceAddress, pfVO.getUuid())
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .select(PciDeviceVO_.uuid)
                .findValue();
        return pciDeviceUuid;
    }

    public static PciDeviceVO getPciDeviceFromPhysicalNicUuid(String physicalNicUuid) {
        HostNetworkInterfaceVO pf = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.uuid, physicalNicUuid)
                .find();
        if (pf == null) {
            return null;
        }

        PciDeviceVO pciDeviceVO = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, pf.getHostUuid())
                .eq(PciDeviceVO_.pciDeviceAddress, pf.getPciDeviceAddress())
                .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                .notEq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                .find();
        return pciDeviceVO;
    }

    public static Integer getHostNetworkInterfaceVirtNum(String hostPhysicalNicUuid) {
        //todo: get physical nic will sync host network insterface spece vo
        return null;
    }
}
