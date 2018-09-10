package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmNicSpec {
    public List<L3NetworkInventory> l3Invs;

    public VmNicSpec(List<L3NetworkInventory> l3Invs) {
        this.l3Invs = l3Invs;
    }

    public VmNicSpec(L3NetworkInventory l3) {
        this.l3Invs = list(l3);
    }

    public static List<L3NetworkInventory> getL3NetworkInventoryOfSpec(List<VmNicSpec> specs) {
        List<L3NetworkInventory> res = new ArrayList<>();
        for (VmNicSpec spec: specs) {
            res.addAll(spec.l3Invs);
        }
        return res;
    }

    public static List<L3NetworkInventory> getFirstL3NetworkInventoryOfSpec(List<VmNicSpec> specs) {
        List<L3NetworkInventory> res = new ArrayList<>();
        for (VmNicSpec spec: specs) {
            if (spec.l3Invs != null && !spec.l3Invs.isEmpty()) {
                res.add(spec.l3Invs.get(0));
            }
        }
        return res;
    }

    public static List<String> getL3UuidsOfSpec(List<VmNicSpec> specs) {
        List<String> res = new ArrayList<>();
        for (VmNicSpec spec: specs) {
            for (L3NetworkInventory inv : spec.l3Invs) {
                res.add(inv.getUuid());
            }
        }
        return res;
    }
}
