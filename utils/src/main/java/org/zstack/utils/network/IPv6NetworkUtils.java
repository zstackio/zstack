package org.zstack.utils.network;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;

import java.math.BigInteger;
import java.util.Arrays;

public class IPv6NetworkUtils {
    private static boolean isConsecutiveRange(BigInteger[] allocatedIps) {
        BigInteger first = allocatedIps[0];
        BigInteger last = allocatedIps[allocatedIps.length - 1];
        return first.add(new BigInteger(String.valueOf(allocatedIps.length - 1))).compareTo(last) == 0;
        //return allocatedIps[allocatedIps.length - 1] - allocatedIps[0] + 1 == allocatedIps.length;
    }

    private static BigInteger findFirstHoleByDichotomy(BigInteger[] allocatedIps) {
        BigInteger first = allocatedIps[0];
        if (isConsecutiveRange(allocatedIps)) {
            String err = "You can not ask me to find a hole in consecutive range!!! ";
            assert false : err;
        }

        if (allocatedIps.length == 2) {
            return first.add(BigInteger.ONE);
        }

        int mIndex = allocatedIps.length / 2;
        BigInteger[] part1 = Arrays.copyOfRange(allocatedIps, 0, mIndex);
        BigInteger[] part2 = Arrays.copyOfRange(allocatedIps, mIndex, allocatedIps.length);
        if (part1.length == 1) {
            if (isConsecutiveRange(part2)) {
                /* For special case there are only three items like [1, 3, 4]*/
                return part1[0].add(BigInteger.ONE);
            } else {
                /* For special case there are only three items like [1, 5, 9] that are all inconsecutive */
                BigInteger[] tmp = new BigInteger[] { part1[0], part2[0] };
                if (!isConsecutiveRange(tmp)) {
                    return part1[0].add(BigInteger.ONE);
                }
            }
        }

        /*For special case that hole is in the middle of array. for example, [1, 2, 4, 5]*/
        if (isConsecutiveRange(part1) && isConsecutiveRange(part2)) {
            return part1[part1.length-1].add(BigInteger.ONE);
        }

        if (!isConsecutiveRange(part1)) {
            return findFirstHoleByDichotomy(part1);
        } else {
            return findFirstHoleByDichotomy(part2);
        }
    }

    private static String IPv6AddressToString(BigInteger ip) {
        return IPv6Address.fromBigInteger(ip).toString();
    }

    // The allocatedIps must be sorted!
    public static BigInteger findFirstAvailableIpv6Address(BigInteger startIp, BigInteger endIp, BigInteger[] allocatedIps) {
        BigInteger ret = null;
        if (startIp.compareTo(endIp) > 0) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid ip range, end ip must be greater than start ip", IPv6AddressToString(startIp), IPv6AddressToString(endIp)));
        }
        if (startIp.equals(endIp) && allocatedIps.length == 0 ) {
            return startIp;
        }
        if (allocatedIps.length == 0) {
            return startIp;
        }

        BigInteger lastAllocatedIp = allocatedIps[allocatedIps.length-1];
        BigInteger firstAllocatedIp = allocatedIps[0];
        if (firstAllocatedIp.compareTo(startIp) < 0 || lastAllocatedIp.compareTo(endIp) > 0) {
            throw new IllegalArgumentException(String.format("[%s, %s] is an invalid allocated ip range, it's not a sub range of ip range[%s, %s]", IPv6AddressToString(firstAllocatedIp), IPv6AddressToString(lastAllocatedIp), IPv6AddressToString(startIp), IPv6AddressToString(endIp)));
        }

        /* ipv4 version: allocatedIps.length == endIp - startIp + 1 */
        if (startIp.add(new BigInteger(String.valueOf(allocatedIps.length))).compareTo(endIp) > 0) {
            /* The ip range is fully occupied*/
            return null;
        }

        if (firstAllocatedIp.compareTo(startIp) > 0) {
            /* The allocatedIps doesn't begin with startIp, then startIp is first available one*/
            return startIp;
        }

        if (isConsecutiveRange(allocatedIps)) {
            /* the allocated ip range is consecutive, allocate the first one out of allocated ip range */
            ret = lastAllocatedIp.add(BigInteger.ONE);
            assert ret.compareTo(endIp) <= 0;
            return ret;
        }

        /* Now the allocated ip range is inconsecutive, we are going to find out the first *hole* in it */
        return findFirstHoleByDichotomy(allocatedIps);
    }

    /* convert 48 bit mac to ipv6 address
     * 1. invert the universal/local bit of mac, universal/local bit is the 6 bit(0 ~7)
     * 2. insert ff:fe after 3rd byte
     * 3. attach converted to mac address to network cidr
     * */
    public static String getIPv6AddresFromMac(String networkCidr, String mac) {
        IPv6Network network = IPv6Network.fromString(networkCidr);
        if (network.getNetmask().asPrefixLength() > 64) {
            return null;
        }

        int idx = networkCidr.indexOf("::");
        String[] macs = mac.split(":");
        return networkCidr.substring(0, idx) + "::" + Integer.toHexString(Integer.parseInt(macs[0], 16) ^ 2) +
                macs[1] + ":" + macs[2] + "ff:fe" + macs[3] +":" + macs[4] + macs[5];
    }
}
