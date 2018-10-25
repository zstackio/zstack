package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VmNicHelper {
    public static List<String> getL3Uuids(VmNicInventory nic) {
        List<String> ret = new ArrayList<>();
        for (UsedIpInventory ip : nic.getUsedIps()) {
            ret.add(ip.getL3NetworkUuid());
        }

        return ret;
    }

    public static boolean isL3AttachedToVmNic(VmNicInventory nic, String l3Uuid) {
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.getL3NetworkUuid().equals(l3Uuid)) {
                return true;
            }
        }
        return false;
    }

    /* where nic ips include all ipuuids input */
    public static boolean nicIncludeAllIps(VmNicInventory nic, List<String> ipUuids) {
        List<String> ips = nic.getUsedIps().stream().map(ip -> ip.getUuid()).collect(Collectors.toList());
        return ips.containsAll(ipUuids);
    }

    /* where input l3 include all nic l3 uuids */
    public static boolean includeAllNicL3s(VmNicInventory nic, List<String> l3Uuids) {
        List<String> l3s = nic.getUsedIps().stream().map(ip -> ip.getL3NetworkUuid()).collect(Collectors.toList());
        return l3Uuids.containsAll(l3s);
    }

    public static List<String> getCanAttachL3List(VmNicInventory nic, List<VmNicSpec> nicSpecs) {
        List<String> res = new ArrayList<>();
        for (VmNicSpec nicspec : nicSpecs) {
            List<String> l3s = nicspec.l3Invs.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList());
            if (l3s.contains(nic.getL3NetworkUuid())) {
                /* remove all ready attached l3 */
                l3s.removeAll(nic.getUsedIps().stream().map(UsedIpInventory::getL3NetworkUuid).collect(Collectors.toList()));
                res.addAll(l3s);
                break;
            }
        }

        return res;
    }

    public static List<String> getCanDetachL3List(VmNicInventory nic, List<VmNicSpec> nicSpecs) {
        List<String> res = new ArrayList<>();
        for (VmNicSpec nicspec : nicSpecs) {
            List<String> l3s = nicspec.l3Invs.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList());
            if (l3s.contains(nic.getL3NetworkUuid())) {
                /* detach all usedIP except the nic first l3*/
                res.addAll(nic.getUsedIps().stream().filter(ip -> !ip.getL3NetworkUuid().equals(nic.getL3NetworkUuid()))
                        .map(UsedIpInventory::getUuid).collect(Collectors.toList()));
                break;
            }
        }

        return res;
    }
}
