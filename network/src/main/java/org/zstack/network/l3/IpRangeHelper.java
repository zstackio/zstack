package org.zstack.network.l3;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.network.l3.*;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class IpRangeHelper {
    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkInventory l3inv) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkInventory l3inv, int ipVersion) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == ipVersion).map(IpRangeInventory::getUuid).collect(Collectors.toList());
        if (uuids.isEmpty()) {
            return new ArrayList<>();
        }
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkVO vo) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkVO vo, int ipVersion) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == ipVersion).map(IpRangeVO::getUuid).collect(Collectors.toList());
        if (uuids.isEmpty()) {
            return new ArrayList<>();
        }
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkInventory l3inv) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkVO vo) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static void updateL3NetworkIpversion(IpRangeVO ipr) {
        updateL3NetworkIpversion(IpRangeInventory.valueOf(ipr));
    }

    public static void updateL3NetworkIpversion(IpRangeInventory ipr) {
        L3NetworkVO l3VO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();
        boolean ipv4 = false;
        boolean ipv6 = false;
        for (IpRangeVO vo : l3VO.getIpRanges()) {
            if (vo.getIpVersion() == IPv6Constants.IPv4) {
                ipv4 = true;
            } else if (vo.getIpVersion() == IPv6Constants.IPv6) {
                ipv6 = true;
            }
        }
        Integer ipVersion = IPv6Constants.NONE;
        if (ipv4 && ipv6) {
            ipVersion = IPv6Constants.DUAL_STACK;
        } else if (ipv4) {
            ipVersion = IPv6Constants.IPv4;
        } else if (ipv6) {
            ipVersion = IPv6Constants.IPv6;
        }

        if (l3VO.getIpVersion() != ipVersion) {
            SQL.New(L3NetworkVO.class).set(L3NetworkVO_.ipVersion, ipVersion).eq(L3NetworkVO_.uuid, l3VO.getUuid()).update();
        }
    }
}
