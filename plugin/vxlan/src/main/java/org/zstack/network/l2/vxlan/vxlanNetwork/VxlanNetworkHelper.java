package org.zstack.network.l2.vxlan.vxlanNetwork;

public class VxlanNetworkHelper {
    public static boolean isValidVniRange(int startVni, int endVni) {
        if (startVni < VxlanNetworkConstant.MIN_VNI || endVni < VxlanNetworkConstant.MIN_VNI) {
            return false;
        }

        if (startVni > VxlanNetworkConstant.MAX_VNI || endVni > VxlanNetworkConstant.MAX_VNI) {
            return false;
        }

        if (startVni > endVni) {
            return false;
        }

        return true;
    }

    public static boolean isValidVlanRange(int startVlan, int endVlan) {
        if (startVlan < VxlanNetworkConstant.MIN_VLAN || endVlan < VxlanNetworkConstant.MIN_VLAN) {
            return false;
        }

        if (startVlan > VxlanNetworkConstant.MAX_VLAN || endVlan > VxlanNetworkConstant.MAX_VLAN) {
            return false;
        }

        if (startVlan > endVlan) {
            return false;
        }

        return true;
    }
}
