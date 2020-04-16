package org.zstack.network.l3;

import org.zstack.core.db.Q;
import org.zstack.header.network.l3.*;

import java.util.List;
import java.util.stream.Collectors;


public class IpRangeHelper {
    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkInventory l3inv) {
        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkVO vo) {
        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkInventory l3inv) {
        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkVO vo) {
        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }
}
